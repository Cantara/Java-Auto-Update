package no.cantara.jau;

import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.util.PropertiesHelper;
import no.cantara.jau.util.ProxyFixer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-13.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        ProxyFixer.fixProxy(PropertiesHelper.getProperties());

        String serviceConfigUrl = PropertiesHelper.getServiceConfigUrl();

        if (serviceConfigUrl == null) {
            log.error("Application cannot start! {} not set in {}.",
                    PropertiesHelper.CONFIG_SERVICE_URL_KEY, PropertiesHelper.CONFIG_FILENAME);
            System.exit(1);
        }

        String clientName = PropertiesHelper.getClientName();
        log.debug("Resolved clientName={}", clientName);

        String username = PropertiesHelper.getUsername();
        String password = PropertiesHelper.getPassword();
        String artifactId = PropertiesHelper.getArtifactId();

        int updateInterval = PropertiesHelper.getUpdateInterval();
        int isRunningInterval = PropertiesHelper.getIsRunningInterval();

        String workingDirectory = "./";

        ConfigServiceClient configServiceClient = new ConfigServiceClient(serviceConfigUrl, username, password);

        new JavaAutoUpdater(configServiceClient, artifactId, workingDirectory, clientName)
                .start(updateInterval, isRunningInterval);
    }
}