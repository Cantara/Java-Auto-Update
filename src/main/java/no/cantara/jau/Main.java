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
            log.debug("Could not load {} from classpath due to {}: {}. \n  Classpath: {}",
                    CONFIG_FILENAME, e.getClass().getSimpleName(), e.getMessage(), System.getProperty("java.class.path"));
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


        registerClient();


        //Start new service
        log.debug("Starting scheduler with an update interval of {} seconds.", updateInterval);
        //The initial extra call to checkForUpdate can be removed, but not a priority now...
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
                        registerClient();
                    } catch (IOException e) {
                        log.error("checkForUpdate failed, do nothing. Retrying in {} seconds.", updateInterval, e);
                        return;
                    }

                    // ExecutorService swallows any exceptions silently, so need to handle them explicitly.
                    // See http://www.nurkiewicz.com/2014/11/executorservice-10-tips-and-tricks.html (point 6.).
                    try {
                        if (newClientConfig == null) {
                            log.debug("No updated config - checking if the process has stopped.");
                        } else {
                            log.debug("We got changes - stopping process and downloading new files.");
                            processHolder.stopProcess();

                            ServiceConfig serviceConfig = newClientConfig.serviceConfig;
                            String workingDirectory = processHolder.getWorkingDirectory().getAbsolutePath();
                            DownloadUtil.downloadAllFiles(serviceConfig.getDownloadItems(), workingDirectory);
                            ConfigurationStoreUtil.toFiles(serviceConfig.getConfigurationStores(), workingDirectory);
                            String[] command = serviceConfig.getStartServiceScript().split("\\s+");
                            processHolder.setCommand(command);
                            processHolder.setLastChangedTimestamp(serviceConfig.getLastChanged());

                            configServiceClient.saveApplicationState(newClientConfig);
                        }
                        if (!processHolder.processIsrunning()) { // Restart, whatever the cause of the shutdown.
                            log.debug("Process is not running - restarting...");
                            processHolder.startProcess();
                        }
                    } catch (Exception e) {
                        log.debug("Error thrown from scheduled lambda.", e);
                    }
                },
                1, updateInterval, SECONDS
        );
    }

    private void registerClient() {
        Properties applicationStateOnStartup = configServiceClient.getApplicationState();
        if (applicationStateOnStartup == null) {
            ClientConfig clientConfig;
            try {
                ClientRegistrationRequest registrationRequest = new ClientRegistrationRequest(artifactId);
                registrationRequest.envInfo.putAll(System.getenv());
                clientConfig = configServiceClient.registerClient(registrationRequest);
                configServiceClient.saveApplicationState(clientConfig);
                //TODO make more robust, e.g. retries
                // ConnectException: Connection refused - retry

                processHolder.setCommand(clientConfig.serviceConfig.getStartServiceScript().split("\\s+"));
                processHolder.setLastChangedTimestamp(clientConfig.serviceConfig.getLastChanged());
            } catch (IOException e) {
                log.error("TODO handle this better", e);
            }
        } else {
            String clientIdOnStartup = getStringProperty(applicationStateOnStartup, ConfigServiceClient.CLIENT_ID, null);
            String lastChangedOnStartup = getStringProperty(applicationStateOnStartup, ConfigServiceClient.LAST_CHANGED, null);
            String commandOnStartup = getStringProperty(applicationStateOnStartup, ConfigServiceClient.COMMAND, null);
            processHolder.setCommand(commandOnStartup.split("\\s+"));
            processHolder.setLastChangedTimestamp(lastChangedOnStartup);
            log.debug("Client already registered. clientId={}, lastChanged={}, command={}", clientIdOnStartup, lastChangedOnStartup, commandOnStartup);
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