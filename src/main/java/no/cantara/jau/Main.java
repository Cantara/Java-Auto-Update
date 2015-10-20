package no.cantara.jau;

import no.cantara.jau.util.PropertiesHelper;
import no.cantara.jau.util.ProxyFixer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-13.
 */
public class Main {

    private static final int DEFAULT_UPDATE_INTERVAL = 3 * 60; // seconds
    private static final String CONFIG_FILENAME = "config.properties";
    private static final String CONFIG_SERVICE_URL_KEY = "configservice.url";
    private static final String CONFIG_SERVICE_USERNAME_KEY = "configservice.username";
    private static final String CONFIG_SERVICE_PASSWORD_KEY = "configservice.password";
    private static final String UPDATE_INTERVAL_KEY = "updateinterval";

    private static Properties properties;

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    // -Dconfigservice.url=http://localhost:8086/jau/clientconfig -Dconfigservice.username=user
    // -Dconfigservice.password=pass -Dconfigservice.artifactid=someArtifactId
    public static void main(String[] args) {
        properties = new Properties();
        try {
            properties.load(Main.class.getClassLoader().getResourceAsStream(CONFIG_FILENAME));
        } catch (NullPointerException | IOException e) {
            log.debug("{} not found on classpath.  Fallback to VM options (-D).", CONFIG_FILENAME);
            //log.debug("{} not found on classpath.  Fallback to -D values. \n  Classpath: {}", CONFIG_FILENAME,
            // System.getProperty("java.class.path"));
        }
        ProxyFixer.fixProxy(properties);
        String serviceConfigUrl = PropertiesHelper.getStringProperty(properties, CONFIG_SERVICE_URL_KEY, null);
        if (serviceConfigUrl == null) {
            log.error("Application cannot start! {} not set in {} or as property (-D{}=).",
                    CONFIG_SERVICE_URL_KEY, CONFIG_FILENAME, CONFIG_SERVICE_URL_KEY);
            System.exit(1);
        }
        String username = PropertiesHelper.getStringProperty(properties, CONFIG_SERVICE_USERNAME_KEY, null);
        String password = PropertiesHelper.getStringProperty(properties, CONFIG_SERVICE_PASSWORD_KEY, null);

        int updateInterval = PropertiesHelper.getIntProperty(properties, UPDATE_INTERVAL_KEY, DEFAULT_UPDATE_INTERVAL);

        String workingDirectory = "./";

        final JavaAutoUpdater javaAutoUpdater = new JavaAutoUpdater(serviceConfigUrl, username, password, workingDirectory);
        javaAutoUpdater.start(updateInterval);
    }



}