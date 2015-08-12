package no.cantara.jau;

import static java.util.concurrent.TimeUnit.*;

import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.serviceconfig.client.ConfigurationStoreUtil;
import no.cantara.jau.serviceconfig.client.DownloadUtil;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-13.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final String CONFIG_FILENAME = "config.properties";

    private static final String CONFIG_SERVICE_URL_KEY = "configservice.url";

    private static final String CONFIG_SERVICE_USERNAME_KEY = "configservice.username";

    private static final String CONFIG_SERVICE_PASSWORD_KEY = "configservice.password";

    private static final String UPDATE_INTERVAL_KEY = "updateinterval";

    public static final int DEFAULT_UPDATE_INTERVAL = 3 * 60; // seconds

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    //String serviceConfigUrl = "http://localhost:8086/jau/serviceconfig/query?clientid=clientid1";
    public static void main(String[] args) {
        final Properties properties = new Properties();
        try {
            properties.load(Main.class.getClassLoader().getResourceAsStream(CONFIG_FILENAME));
        } catch (NullPointerException | IOException e) {
            log.debug("Could not load {} from classpath due to {}: {}. \n  Classpath: {}",
                      CONFIG_FILENAME, e.getClass().getSimpleName(), e.getMessage(),
                      System.getProperty("java.class.path"));
        }
        String serviceConfigUrl = getStringProperty(properties, CONFIG_SERVICE_URL_KEY, null);
        if (serviceConfigUrl == null) {
            log.error("Application cannot start! {} not set in {} or as property (-D{}=).",
                      CONFIG_SERVICE_URL_KEY, CONFIG_FILENAME, CONFIG_SERVICE_URL_KEY);
            System.exit(1);
        }
        String username = getStringProperty(properties, CONFIG_SERVICE_USERNAME_KEY, null);
        String password = getStringProperty(properties, CONFIG_SERVICE_PASSWORD_KEY, null);
        int updateInterval = getIntProperty(properties, UPDATE_INTERVAL_KEY, DEFAULT_UPDATE_INTERVAL);

        String workingDirectory = "./";
        final Main main = new Main();
        main.start(serviceConfigUrl, username, password, workingDirectory, updateInterval);
    }

    /*
    lib/wrapper.jar:config_override:lib/java-auto-update-1.0-SNAPSHOT.jar:lib/configservice-sdk-1.0-SNAPSHOT.jar:lib/jackson-databind-2.5.3.jar:lib/jackson-annotations-2.5.0.jar:lib/jackson-core-2.5.3.jar:lib/slf4j-api-1.7.12.jar:lib/logback-classic-1.1.3.jar:lib/logback-core-1.1.3.jar
     */
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

    /**
     * Fetch ServiceConfig
     * Parse ServiceConfig
     * Check changedTimestamp
     * Download
     * Stop existing service if running
     * Start new service
     */
    public void start(String serviceConfigUrl, String username, String password, String workingDirectory,
                      int updateInterval) {

        //Stop existing service if running
        // https://github.com/Cantara/Java-Auto-Update/issues/4

        //Start new service
        final ApplicationProcess processHolder = new ApplicationProcess(); // Because of Java 8's "final" limitation on closures, any outside variables that need to be changed inside the closure must be wrapped in a final object.

        log.debug("Starting scheduler with an update interval of {} seconds.", updateInterval);
        final ScheduledFuture<?> restarterHandle = scheduler.scheduleAtFixedRate(
                () -> {

                    ServiceConfig serviceConfig = null;
                    try {
                        serviceConfig = ConfigServiceClient
                                .fetchAndParseServiceConfig(serviceConfigUrl, username, password);
                    } catch (IOException e) {
                        log.error("fetchServiceConfig failed with serviceConfigUrl={} Exiting.", serviceConfigUrl, e);
                        System.exit(2);
                    }
                    try { // ExecutorService swallows any exceptions silently, so need to handle them explicitly. See http://www.nurkiewicz.com/2014/11/executorservice-10-tips-and-tricks.html (point 6.).
                        String changedTimestamp = serviceConfig.getChangedTimestamp();
                        if (changedTimestamp.equals(processHolder.getLastChangedTimestamp())) {
                            log.debug("Timestamp has not changed - no action needed.");
                            return;
                        }
                        log.debug("We got changes - downloading new files and restarting process.");
                        processHolder.setLastChangedTimestamp(changedTimestamp);
                        DownloadUtil.downloadAllFiles(serviceConfig.getDownloadItems(), workingDirectory);
                        ConfigurationStoreUtil.toFiles(serviceConfig.getConfigurationStores(), workingDirectory);
                        String[] command = serviceConfig.getStartServiceScript().split("\\s+");
                        processHolder.setCommand(command);
                        processHolder.setWorkingDirectory(new File(workingDirectory));
                        processHolder.reStartProcess();
                    } catch (Exception e) {
                        log.debug("Error thrown from scheduled lambda.", e);
                    }
                },
                1, updateInterval, SECONDS
        );

    }

}
