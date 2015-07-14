package no.cantara.jau;

import no.cantara.jau.serviceconfig.dto.DownloadItem;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;
import no.cantara.jau.serviceconfig.dto.ServiceConfigSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-13.
 */
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private final ExecutorService worker = Executors.newSingleThreadExecutor();


    public static void main(String[] args) {
        String serviceConfigUrl = "http://localhost:7000/jau/serviceconfig/query?clientid=clientid1";
        String workingDirectory = "./";

        final Main main = new Main();
        main.start(serviceConfigUrl, workingDirectory);
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
        Path path = null;
        for (DownloadItem downloadItem : serviceConfig.getDownloadItems()) {
            log.debug("Downloading {}", downloadItem);
            path = DownloadUtil.downloadFile(downloadItem.url, workingDirectory, downloadItem.filename());
        }

        //Stop existing service if running


        //Start new service
        //String jarPath = path.toString();
        //new JarProcess("-DIAM_MODE=DEV", jarPath).run();
        //String commandAsString = "java -DIAM_MODE=DEV -jar" + path.getFileName().toString();
        String commandAsString = serviceConfig.getStartServiceScript();
        String[] command = commandAsString.split("\\s+");
        Future<?> future = worker.submit(new ApplicationProcess(workingDirectory, command));
    }
}
