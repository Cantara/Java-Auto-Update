package no.cantara.jau;

import no.cantara.jau.serviceconfig.dto.DownloadItem;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;
import no.cantara.jau.serviceconfig.dto.ServiceConfigSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-13.
 */
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private final ExecutorService worker = Executors.newSingleThreadExecutor();


    public static void main(String[] args) {
        String serviceConfigUrl = "http://localhost:7000/jau/serviceconfig/query?clientid=clientid1";
        final Main main = new Main();
        main.start(serviceConfigUrl);

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
     */
    public void start(String serviceConfigUrl) {
        String response = null;
        try {
            response = ConfigServiceClient.fetchServiceConfig(serviceConfigUrl);
            log.trace("fetchServiceConfig: serviceConfig={}", response);
        } catch (Exception e) {
            log.error("fetchServiceConfig failed with serviceConfigUrl={} Exiting.", serviceConfigUrl, e);
            System.exit(1);
        }

        //Parse
        ServiceConfig serviceConfig = ServiceConfigSerializer.fromJson(response);
        log.debug("{}", serviceConfig);


        //check changedTimestamp

        //Download
        for (DownloadItem downloadItem : serviceConfig.getDownloadItems()) {
            log.debug("Downloading {}", downloadItem);
            DownloadUtil.downloadFile(downloadItem.getUrl(), "/tmp", downloadItem.getFilename());
        }

        //Stop existing service if running

        //Start new service
    }

}
