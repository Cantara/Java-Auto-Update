package no.cantara.jau;

import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.serviceconfig.client.DownloadUtil;
import no.cantara.jau.serviceconfig.dto.DownloadItem;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;
import no.cantara.jau.serviceconfig.dto.ServiceConfigSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-13.
 */
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final String CONFIG_SERVICE_URL_KEY = "configServiceUrl";
    private static final String CONFIG_FILENAME = "config.properties";

    private final ExecutorService worker = Executors.newSingleThreadExecutor();


    //String serviceConfigUrl = "http://localhost:7000/jau/serviceconfig/query?clientid=clientid1";
    public static void main(String[] args) {
        String serviceConfigUrl = getServiceConfigUrlOrExit();

        String workingDirectory = "./";
        final Main main = new Main();
        main.start(serviceConfigUrl, workingDirectory);
    }

    
    /*
    lib/wrapper.jar:config_override:lib/java-auto-update-1.0-SNAPSHOT.jar:lib/configservice-sdk-1.0-SNAPSHOT.jar:lib/jackson-databind-2.5.3.jar:lib/jackson-annotations-2.5.0.jar:lib/jackson-core-2.5.3.jar:lib/slf4j-api-1.7.12.jar:lib/logback-classic-1.1.3.jar:lib/logback-core-1.1.3.jar
     */
    private static String getServiceConfigUrlOrExit() {
        String serviceConfigUrl = null;
        final Properties properties = new Properties();
        try {
            properties.load(Main.class.getClassLoader().getResourceAsStream(CONFIG_FILENAME));
            serviceConfigUrl = properties.getProperty(CONFIG_SERVICE_URL_KEY);
        } catch (NullPointerException|IOException e) {
            log.debug("Could not load {} from classpath due to {}: {}. \n  Classpath: {}",
                    CONFIG_FILENAME, e.getClass().getSimpleName(), e.getMessage(), System.getProperty("java.class.path"));
        }

        if (serviceConfigUrl == null) {
            //-DconfigServiceUrl=http://localhost:7000/jau/serviceconfig/query?clientid=clientid1
            serviceConfigUrl = System.getProperty(CONFIG_SERVICE_URL_KEY);
        }

        if (serviceConfigUrl == null) {
            log.error("Application cannot start! {} not set in {} or as property (-DconfigServiceUrl=http://localhost:7000/jau/serviceconfig/query?clientid=clientid1).",
                    CONFIG_SERVICE_URL_KEY, CONFIG_FILENAME);
            System.exit(1);
        }
        return serviceConfigUrl;
    }

    /**
     * Fetch ServiceConfig
     * Parse ServiceConfig
     * Check changedTimestamp
     * Download
     * Stop existing service if running
     * Start new service
     *
     * @param serviceConfigUrl  url to service config for this service
     * @param workingDirectory
     */
    public void start(String serviceConfigUrl, String workingDirectory) {
        // https://github.com/Cantara/ConfigService/issues/3
        String response = null;
        try {
            response = ConfigServiceClient.fetchServiceConfig(serviceConfigUrl);
            log.trace("fetchServiceConfig: serviceConfig={}", response);
        } catch (Exception e) {
            log.error("fetchServiceConfig failed with serviceConfigUrl={} Exiting.", serviceConfigUrl, e);
            System.exit(2);
        }

        //Parse
        ServiceConfig serviceConfig = ServiceConfigSerializer.fromJson(response);
        log.debug("{}", serviceConfig);


        //check changedTimestamp
        // https://github.com/Cantara/Java-Auto-Update/issues/5

        //Download
        Path path = null;
        for (DownloadItem downloadItem : serviceConfig.getDownloadItems()) {
            log.debug("Downloading {}", downloadItem);
            path = DownloadUtil.downloadFile(downloadItem, workingDirectory);
        }

        //Stop existing service if running
        // https://github.com/Cantara/Java-Auto-Update/issues/4


        //Start new service
        String commandAsString = serviceConfig.getStartServiceScript();
        String[] command = commandAsString.split("\\s+");
        Future<?> future = worker.submit(new ApplicationProcess(workingDirectory, command));
    }
}
