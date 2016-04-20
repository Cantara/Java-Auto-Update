package no.cantara.jau;

import no.cantara.cs.client.ConfigServiceClient;
import no.cantara.cs.client.ConfigurationStoreUtil;
import no.cantara.cs.client.DownloadUtil;
import no.cantara.cs.dto.ApplicationConfig;
import no.cantara.cs.dto.ClientConfig;
import no.cantara.jau.coms.CheckForUpdateHelper;
import no.cantara.jau.coms.RegisterClientHelper;
import no.cantara.jau.duplicatehandler.DuplicateProcessHandler;
import no.cantara.jau.eventextraction.EventExtractorService;
import no.cantara.jau.util.PropertiesHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.SECONDS;

public class JavaAutoUpdater {

    private static final Logger log = LoggerFactory.getLogger(JavaAutoUpdater.class);

    private static ScheduledFuture<?> processMonitorHandle;
    private static ScheduledFuture<?> updaterHandle;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ConfigServiceClient configServiceClient;
    private final ApplicationProcess processHolder;
    private final DuplicateProcessHandler duplicateProcessHandler;
    private final EventExtractorService extractorService;

    private RegisterClientHelper registerClientHelper;

    public JavaAutoUpdater(ConfigServiceClient configServiceClient, RegisterClientHelper registerClientHelper,
                           ApplicationProcess applicationProcess, DuplicateProcessHandler duplicateProcessHandler,
                           EventExtractorService extractorService) {
        this.configServiceClient = configServiceClient;
        this.registerClientHelper = registerClientHelper;
        this.processHolder = applicationProcess;
        this.duplicateProcessHandler = duplicateProcessHandler;
        this.extractorService = extractorService;

        addShutdownHook();
    }

    /**
     * Registers a shutdown hook that attempts to stop the application process when JAU is stopped.
     */
    private void addShutdownHook() {
        if (PropertiesHelper.stopApplicationOnShutdown()) {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    processHolder.stopProcess();
                }
            });
            log.info("Registered shutdown hook for stopping application.");
        }
    }

    /**
     * registerClient
     * checkForUpdate
     * if changed
     * Download
     * Stop existing service if running
     * Start new service
     */
    public void start(int updateInterval, int isRunningInterval) {
        // registerClient or fetch applicationState from file
        if (configServiceClient.getApplicationState() == null) {
            ClientConfig clientConfig = registerClient();
            storeClientFiles(clientConfig);
        } else {
            log.debug("Client already registered. Skip registerClient and use properties from file.");
        }

        Properties initialApplicationState = configServiceClient.getApplicationState();
        initializeProcessHolder(initialApplicationState);
        extractorService.updateConfigs(configServiceClient.getEventExtractionConfigs());

        // checkForUpdate and start process
        while (true) {
            int sleepTime = 10000;
            if (updaterHandle == null || updaterHandle.isCancelled() || updaterHandle.isDone()) {
                updaterHandle = startUpdaterThread(updateInterval);
            }

            if (processMonitorHandle == null || processMonitorHandle.isCancelled() || processMonitorHandle.isDone()) {
                String processCommand = configServiceClient.getApplicationState().getProperty("command");
                boolean successKillingProcess = duplicateProcessHandler.killExistingProcessIfRunning(processCommand);

                if (!successKillingProcess) {
                    log.error("Problem killing running process! A new managed process will not be started. " +
                              "Retrying in {} seconds", sleepTime / 1000);
                } else {
                    processMonitorHandle = startProcessMonitorThread(isRunningInterval);
                }
            }

            // make sure everything runs, forever
            try {
                Thread.sleep(sleepTime);
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
                    if (!processHolder.processIsRunning()) {
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
        return scheduler.scheduleWithFixedDelay(
                CheckForUpdateHelper.getCheckForUpdateRunnable(interval, configServiceClient, processHolder,
                                                               processMonitorHandle, extractorService, this),
                1, interval, SECONDS
                                               );
    }

    public ClientConfig registerClient() {
        return registerClientHelper.registerClient();
    }

    public void storeClientFiles(ClientConfig clientConfig) {
        String workingDirectory = processHolder.getWorkingDirectory().getAbsolutePath();
        ApplicationConfig config = clientConfig.config;
        DownloadUtil.downloadAllFiles(config.getDownloadItems(), workingDirectory);
        ConfigurationStoreUtil.toFiles(config.getConfigurationStores(), workingDirectory);
        extractorService.updateConfigs(config.getEventExtractionConfigs());
    }

    private void initializeProcessHolder(Properties initialApplicationState) {
        String initialClientId = PropertiesHelper.getStringProperty(initialApplicationState, ConfigServiceClient.CLIENT_ID, null);
        String initialLastChanged = PropertiesHelper.getStringProperty(initialApplicationState, ConfigServiceClient.LAST_CHANGED, null);
        String initialCommand = PropertiesHelper.getStringProperty(initialApplicationState, ConfigServiceClient.COMMAND, null);
        Properties environment = PropertiesHelper.getPropertiesFromConfigFile(PropertiesHelper.APPLICATION_ENV_FILENAME);

        processHolder.setCommand(initialCommand.split("\\s+"));
        processHolder.setClientId(initialClientId);
        processHolder.setLastChangedTimestamp(initialLastChanged);
        processHolder.setEnvironment(PropertiesHelper.propertiesAsMap(environment));
    }

}
