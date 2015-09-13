package no.cantara.jau;

import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.serviceconfig.client.ConfigurationStoreUtil;
import no.cantara.jau.serviceconfig.client.DownloadUtil;
import no.cantara.jau.serviceconfig.dto.ClientConfig;
import no.cantara.jau.serviceconfig.dto.ClientRegistrationRequest;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-13.
 */
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final String CONFIG_FILENAME = "config.properties";
    private static final String CONFIG_SERVICE_URL_KEY = "configservice.url";
    private static final String CONFIG_SERVICE_USERNAME_KEY = "configservice.username";
    private static final String CONFIG_SERVICE_PASSWORD_KEY = "configservice.password";
    private static final String ARTIFACT_ID = "configservice.artifactid";
    private static final String UPDATE_INTERVAL_KEY = "updateinterval";
    public static final int DEFAULT_UPDATE_INTERVAL = 3 * 60; // seconds

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ConfigServiceClient configServiceClient;
    private final String artifactId;
    private final ApplicationProcess processHolder;


    public Main(String serviceConfigUrl, String username, String password, String artifactId, String workingDirectory) {
        configServiceClient = new ConfigServiceClient(serviceConfigUrl, username, password);
        this.artifactId = artifactId;
        // Because of Java 8's "final" limitation on closures, any outside variables that need to be changed inside the closure must be wrapped in a final object.
        processHolder = new ApplicationProcess();
        processHolder.setWorkingDirectory(new File(workingDirectory));
    }

    //-Dconfigservice.url=http://localhost:8086/jau/clientconfig -Dconfigservice.username=user -Dconfigservice.password=pass -Dconfigservice.artifactid=someArtifactId
    public static void main(String[] args) {
        final Properties properties = new Properties();
        try {
            properties.load(Main.class.getClassLoader().getResourceAsStream(CONFIG_FILENAME));
        } catch (NullPointerException | IOException e) {
            log.debug("{} not found on classpath.  Fallback to VM options (-D).", CONFIG_FILENAME);
            //log.debug("{} not found on classpath.  Fallback to -D values. \n  Classpath: {}", CONFIG_FILENAME, System.getProperty("java.class.path"));
        }
        String serviceConfigUrl = getStringProperty(properties, CONFIG_SERVICE_URL_KEY, null);
        if (serviceConfigUrl == null) {
            log.error("Application cannot start! {} not set in {} or as property (-D{}=).",
                    CONFIG_SERVICE_URL_KEY, CONFIG_FILENAME, CONFIG_SERVICE_URL_KEY);
            System.exit(1);
        }
        String username = getStringProperty(properties, CONFIG_SERVICE_USERNAME_KEY, null);
        String password = getStringProperty(properties, CONFIG_SERVICE_PASSWORD_KEY, null);
        String artifactId = getStringProperty(properties, ARTIFACT_ID, null);

        int updateInterval = getIntProperty(properties, UPDATE_INTERVAL_KEY, DEFAULT_UPDATE_INTERVAL);

        String workingDirectory = "./";
        final Main main = new Main(serviceConfigUrl, username, password, artifactId, workingDirectory);
        main.start(updateInterval);
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
        //Stop existing service if running
        //https://github.com/Cantara/Java-Auto-Update/issues/4

        //registerClient or fetch applicationState from file
        if (configServiceClient.getApplicationState() == null) {
            ClientConfig clientConfig = registerClient();

            String workingDirectory = processHolder.getWorkingDirectory().getAbsolutePath();
            ServiceConfig serviceConfig = clientConfig.serviceConfig;
            DownloadUtil.downloadAllFiles(serviceConfig.getDownloadItems(), workingDirectory);
            ConfigurationStoreUtil.toFiles(serviceConfig.getConfigurationStores(), workingDirectory);
        } else {
            log.debug("Client already registered. Skip registerClient and use properties from file.");
        }
        Properties initialApplicationState = configServiceClient.getApplicationState();
        String initialClientId = getStringProperty(initialApplicationState, ConfigServiceClient.CLIENT_ID, null);
        String initialLastChanged = getStringProperty(initialApplicationState, ConfigServiceClient.LAST_CHANGED, null);
        String initialCommand = getStringProperty(initialApplicationState, ConfigServiceClient.COMMAND, null);
        processHolder.setCommand(initialCommand.split("\\s+"));
        processHolder.setClientId(initialClientId);
        processHolder.setLastChangedTimestamp(initialLastChanged);


        //checkForUpdate and start process
        log.debug("Starting scheduler with an update interval of {} seconds.", updateInterval);
        final ScheduledFuture<?> restarterHandle = scheduler.scheduleAtFixedRate(
                () -> {
                    ClientConfig newClientConfig = null;
                    try {
                        Properties applicationState = configServiceClient.getApplicationState();
                        String clientId = getStringProperty(applicationState, ConfigServiceClient.CLIENT_ID, null);
                        String lastChanged = getStringProperty(applicationState, ConfigServiceClient.LAST_CHANGED, null);
                        newClientConfig = configServiceClient.checkForUpdate(clientId, lastChanged, System.getenv());
                    } catch (IllegalStateException regE) {
                        log.warn(regE.getMessage());
                        configServiceClient.cleanApplicationState();
                        newClientConfig = registerClient();
                    } catch (IOException e) {
                        log.error("checkForUpdate failed, do nothing. Retrying in {} seconds.", updateInterval, e);
                        return;
                    }

                    // ExecutorService swallows any exceptions silently, so need to handle them explicitly.
                    // See http://www.nurkiewicz.com/2014/11/executorservice-10-tips-and-tricks.html (point 6.).
                    try {
                        if (newClientConfig == null) {
                            log.debug("No updated config. - checking if the process has stopped.");
                        } else {
                            log.debug("We got changes - stopping process and downloading new files.");
                            processHolder.stopProcess();

                            String workingDirectory = processHolder.getWorkingDirectory().getAbsolutePath();
                            ServiceConfig serviceConfig = newClientConfig.serviceConfig;
                            DownloadUtil.downloadAllFiles(serviceConfig.getDownloadItems(), workingDirectory);
                            ConfigurationStoreUtil.toFiles(serviceConfig.getConfigurationStores(), workingDirectory);
                            String[] command = serviceConfig.getStartServiceScript().split("\\s+");
                            processHolder.setCommand(command);
                            processHolder.setClientId(newClientConfig.clientId);
                            processHolder.setLastChangedTimestamp(serviceConfig.getLastChanged());

                            configServiceClient.saveApplicationState(newClientConfig);
                        }

                        // Restart, whatever the reason the process is not running.
                        if (!processHolder.processIsrunning()) {
                            log.debug("Process is not running - restarting... clientId={}, lastChanged={}, command={}",
                                    processHolder.getClientId(), processHolder.getLastChangedTimestamp(), processHolder.getCommand());
                            processHolder.startProcess();
                        }
                    } catch (Exception e) {
                        log.debug("Error thrown from scheduled lambda.", e);
                    }
                },
                1, updateInterval, SECONDS
        );
    }

    private ClientConfig registerClient() {
        ClientConfig clientConfig;
        try {
            ClientRegistrationRequest registrationRequest = new ClientRegistrationRequest(artifactId);
            registrationRequest.envInfo.putAll(System.getenv());
            clientConfig = configServiceClient.registerClient(registrationRequest);
            configServiceClient.saveApplicationState(clientConfig);
            return clientConfig;
            //TODO make more robust, e.g. retries
            // ConnectException: Connection refused - retry
        } catch (IOException e) {
            log.error("TODO handle this better", e);
            throw new RuntimeException(e);
        }
    }


    private static String getStringProperty(final Properties properties, String propertyKey, String defaultValue) {
        String property = properties.getProperty(propertyKey, defaultValue);
        if (property == null) {
            //-Dconfigservice.url=
            property = System.getProperty(propertyKey);
        }
        return property;
    }
    private static Integer getIntProperty(final Properties properties, String propertyKey, Integer defaultValue) {
        String property = getStringProperty(properties, propertyKey, null);
        if (property == null) {
            return defaultValue;
        }
        return Integer.valueOf(property);
    }
}