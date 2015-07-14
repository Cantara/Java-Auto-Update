package no.cantara.jau;

import no.cantara.jau.serviceconfig.dto.DownloadItem;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;
import no.cantara.jau.serviceconfig.dto.ServiceConfigSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Not working, just loose thoughts on how to run in-process.
 *
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-13.
 */
public class MainInProcess {
    private static final Logger log = LoggerFactory.getLogger(MainInProcess.class);

    private final ExecutorService worker = Executors.newSingleThreadExecutor();


    public static void main(String[] args) {
        String serviceConfigUrl = "http://localhost:7000/jau/serviceconfig/query?clientid=clientid1";
        final MainInProcess main = new MainInProcess();
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
        Path path = null;
        for (DownloadItem downloadItem : serviceConfig.getDownloadItems()) {
            log.debug("Downloading {}", downloadItem);
            path = DownloadUtil.downloadFile(downloadItem.url, "./", downloadItem.filename());
        }

        //Stop existing service if running

        //Start new service

        //"file://./my.jar"
        try {
            URL url = path.toUri().toURL();

            /*
            Class<?> mainClass = getClass().getClassLoader().loadClass("net.whydah.admin.MainWithJetty");
            Method method = mainClass.getDeclaredMethod("main");
            Object result = method.invoke(null);
            */

            ClassLoader loader = URLClassLoader.newInstance(new URL[]{url}, getClass().getClassLoader());
            Class<?> clazz = Class.forName("net.whydah.admin.MainWithJetty", true, loader);

            // Avoid Class.newInstance, for it is evil.
            //Constructor<?> constructor = clazz.getConstructor(int.class);
            //Object instance = constructor.newInstance(5678);
            Method method = clazz.getDeclaredMethod("main", String[].class);
            // If the underlying method is static, then the specified obj argument is ignored. It may be null.
            Object result = method.invoke(null, new Object[] {new String[]{}});



            /*
            ClassLoader loader = URLClassLoader.newInstance(new URL[]{url}, getClass().getClassLoader());
            Class<?> clazz = Class.forName("net.whydah.admin.MainWithJetty", true, loader);

            Class<? extends Runnable> runClass = clazz.asSubclass(Runnable.class);
            // Avoid Class.newInstance, for it is evil.
            Constructor<? extends Runnable> constructor = runClass.getConstructor(int.class);
            Runnable runnable = constructor.newInstance(5678);
            */

            //runnable.run();

            /*
            URLClassLoader classLoader = new URLClassLoader(new URL[]{url}, this.getClass().getClassLoader());
            Class classToLoad = Class.forName("net.whydah.admin.MainWithJetty", true, classLoader);
            Runnable instance = (Runnable) classToLoad.newInstance();
            */
            //worker.submit(runnable);

            //Method method = classToLoad.getDeclaredMethod("myMethod");
            //Object result = method.invoke(instance);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
