package no.cantara.jau;

import no.cantara.jau.coms.RegisterClientHelper;
import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.serviceconfig.client.ConfigurationStoreUtil;
import no.cantara.jau.serviceconfig.client.DownloadUtil;
import no.cantara.jau.serviceconfig.dto.ClientConfig;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;
import no.cantara.jau.util.ClientEnvironmentUtil;
import no.cantara.jau.util.PropertiesHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.SortedMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.SECONDS;

public class JavaAutoUpdater {

    private static final Logger log = LoggerFactory.getLogger(JavaAutoUpdater.class);

    private static ScheduledFuture<?> processMonitorHandle;
    private static ScheduledFuture<?> updaterHandle;

    private final int isRunningCheckInterval = 10; // seconds

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ConfigServiceClient configServiceClient;
    private final ApplicationProcess processHolder;

    private final String serviceConfigUrl;
    private final String artifactId;

    public JavaAutoUpdater(String serviceConfigUrl, String username, String password, String artifactId, String workingDirectory) {
        this.serviceConfigUrl = serviceConfigUrl;
        this.configServiceClient = new ConfigServiceClient(serviceConfigUrl, username, password);
        this.artifactId = artifactId;
        // Because of Java 8's "final" limitation on closures, any outside variables that need to be changed inside the
        // closure must be wrapped in a final object.
        processHolder = new ApplicationProcess();
        processHolder.setWorkingDirectory(new File(workingDirectory));
    }

    /**
     * registerClient
     * checkForUpdate
     * if changed
     *   Download
     *   Stop existing service if running
     *   Start new service
     */
    public void start(int updateInterval) {
        // TODO: Stop existing service if running
        // https://github.com/Cantara/Java-Auto-Update/issues/4

        // registerClient or fetch applicationState from file
        if (configServiceClient.getApplicationState() == null) {
            ClientConfig clientConfig = registerClient();
            storeClientFiles(clientConfig);
        } else {
            log.debug("Client already registered. Skip registerClient and use properties from file.");
        }

        Properties initialApplicationState = configServiceClient.getApplicationState();
        initializeProcessHolder(initialApplicationState);

        // checkForUpdate and start process
        while (true) {
            if (updaterHandle == null || updaterHandle.isCancelled() || updaterHandle.isDone()) {
                updaterHandle = startUpdaterThread(updateInterval);
            }

            if (processMonitorHandle == null || processMonitorHandle.isCancelled() || processMonitorHandle.isDone()) {
                processMonitorHandle = startProcessMonitorThread(isRunningCheckInterval);
            }

            // make sure everything runs, forever
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                log.warn("Thread was interrupted", e);
            }
        }
    }

    private ScheduledFuture<?> startProcessMonitorThread(long interval) {
        log.debug("Starting process monitoring scheduler with an update interval of {} seconds.", interval);
        return scheduler.scheduleAtFixedRate(
                () -> {
                    log.debug("Checking if process is running...");

                    // Restart, whatever the reason the process is not running.
                    if (!processHolder.processIsrunning()) {
                        log.debug("Process is not running - restarting... clientId={}, lastChanged={}, command={}",
                                processHolder.getClientId(), processHolder.getLastChangedTimestamp(), processHolder.getCommand());

                        processHolder.startProcess();
                    }
                },
                1, interval, SECONDS
        );
    }

    private ScheduledFuture<?> startUpdaterThread(long interval) {
        log.debug("Starting update scheduler with an update interval of {} seconds.", interval);
        return scheduler.scheduleAtFixedRate(
                () -> {
                    ClientConfig newClientConfig = null;
                    try {
                        // check for update
                        Properties applicationState = configServiceClient.getApplicationState();
                        String clientId = PropertiesHelper.getStringProperty(applicationState, ConfigServiceClient.CLIENT_ID, null);
                        String lastChanged = PropertiesHelper.getStringProperty(applicationState, ConfigServiceClient.LAST_CHANGED, null);
                        SortedMap<String, String> clientEnvironment = ClientEnvironmentUtil.getClientEnvironment();
                        newClientConfig = configServiceClient.checkForUpdate(clientId, lastChanged, clientEnvironment);
                    } catch (IllegalStateException e) {
                        // illegal state - reregister client
                        log.warn(e.getMessage());
                        configServiceClient.cleanApplicationState();
                        newClientConfig = registerClient();
                    } catch (IOException e) {
                        log.error("checkForUpdate failed, do nothing. Retrying in {} seconds.", interval, e);
                        return;
                    }

                    if (newClientConfig == null) {
                        log.debug("No updated config.");
                        return;
                    }

                    // ExecutorService swallows any exceptions silently, so need to handle them explicitly.
                    // See http://www.nurkiewicz.com/2014/11/executorservice-10-tips-and-tricks.html (point 6.).
                    try {
                        log.debug("We got changes - stopping process and downloading new files.");
                        processHolder.stopProcess();

                        storeClientFiles(newClientConfig);

                        String[] command = newClientConfig.serviceConfig.getStartServiceScript().split("\\s+");
                        processHolder.setCommand(command);
                        processHolder.setClientId(newClientConfig.clientId);
                        processHolder.setLastChangedTimestamp(newClientConfig.serviceConfig.getLastChanged());

                        configServiceClient.saveApplicationState(newClientConfig);

                        processMonitorHandle.notify();
                    } catch (Exception e) {
                        log.debug("Error thrown from scheduled lambda.", e);
                    }
                },
                1, interval, SECONDS
        );
    }

    private ClientConfig registerClient() {
        Properties applicationState = configServiceClient.getApplicationState();
        String clientName = applicationState == null ? null : PropertiesHelper.getClientNameFromProperties(applicationState);
        RegisterClientHelper registerClientHelper = new RegisterClientHelper(configServiceClient, artifactId,
                clientName, serviceConfigUrl);
        return registerClientHelper.registerClient();
    }

    private void storeClientFiles(ClientConfig clientConfig) {
        String workingDirectory = processHolder.getWorkingDirectory().getAbsolutePath();
        ServiceConfig serviceConfig = clientConfig.serviceConfig;
        DownloadUtil.downloadAllFiles(serviceConfig.getDownloadItems(), workingDirectory);
        ConfigurationStoreUtil.toFiles(serviceConfig.getConfigurationStores(), workingDirectory);
    }

    private void initializeProcessHolder(Properties initialApplicationState) {
        String initialClientId = PropertiesHelper.getStringProperty(initialApplicationState, ConfigServiceClient.CLIENT_ID, null);
        String initialLastChanged = PropertiesHelper.getStringProperty(initialApplicationState, ConfigServiceClient.LAST_CHANGED, null);
        String initialCommand = PropertiesHelper.getStringProperty(initialApplicationState, ConfigServiceClient.COMMAND, null);
        processHolder.setCommand(initialCommand.split("\\s+"));
        processHolder.setClientId(initialClientId);
        processHolder.setLastChangedTimestamp(initialLastChanged);
    }

}
