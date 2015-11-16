package no.cantara.jau;

import no.cantara.jau.coms.RegisterClientHelper;
import no.cantara.jau.processkill.DuplicateProcessHandler;
import no.cantara.jau.processkill.LastRunningProcessFileUtil;
import no.cantara.jau.processkill.ProcessExecutorFetcher;
import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.util.PropertiesHelper;
import no.cantara.jau.util.ProxyFixer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-13.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        ProxyFixer.fixProxy(PropertiesHelper.getPropertiesFromConfigFile(PropertiesHelper.CONFIG_FILENAME));

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

        ConfigServiceClient configServiceClient = new ConfigServiceClient(serviceConfigUrl, username, password);
        RegisterClientHelper registerClientHelper = new RegisterClientHelper(configServiceClient, artifactId, clientName);

        ProcessExecutorFetcher processExecutorFetcher = new ProcessExecutorFetcher();
        LastRunningProcessFileUtil fileUtil = new LastRunningProcessFileUtil(
                DuplicateProcessHandler.RUNNING_PROCESS_FILENAME);
        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(processExecutorFetcher, fileUtil);

        String workingDirectory = "./";
        ApplicationProcess processHolder = new ApplicationProcess(duplicateProcessHandler);
        processHolder.setWorkingDirectory(new File(workingDirectory));

        new JavaAutoUpdater(configServiceClient, registerClientHelper, processHolder, duplicateProcessHandler)
                .start(updateInterval, isRunningInterval);
    }
}