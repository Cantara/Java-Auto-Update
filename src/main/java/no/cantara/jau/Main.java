package no.cantara.jau;

import no.cantara.cs.client.ConfigServiceClient;
import no.cantara.jau.coms.RegisterClientHelper;
import no.cantara.jau.duplicatehandler.DuplicateProcessHandler;
import no.cantara.jau.duplicatehandler.LastRunningProcessFileUtil;
import no.cantara.jau.duplicatehandler.ProcessExecutorFetcher;
import no.cantara.jau.eventextraction.EventExtractorService;
import no.cantara.jau.eventextraction.EventRepo;
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
        ProxyFixer.fixProxy(PropertiesHelper.getPropertiesFromConfigFile(PropertiesHelper.JAU_CONFIG_FILENAME));

        String configServiceUrl = PropertiesHelper.getConfigServiceUrl();

        if (configServiceUrl == null) {
            log.error("Application cannot start! {} not set in {}.",
                    PropertiesHelper.CONFIG_SERVICE_URL_KEY, PropertiesHelper.JAU_CONFIG_FILENAME);
            System.exit(1);
        }

        String clientName = PropertiesHelper.getClientName();
        String username = PropertiesHelper.getUsername();
        String password = PropertiesHelper.getPassword();
        String artifactId = PropertiesHelper.getArtifactId();
        String clientId = PropertiesHelper.getClientId();
        String startPattern = PropertiesHelper.getStartPattern();

        log.debug("Resolved clientId={}", clientId);

        int updateInterval = PropertiesHelper.getUpdateInterval();
        int isRunningInterval = PropertiesHelper.getIsRunningInterval();

        ConfigServiceClient configServiceClient = new ConfigServiceClient(configServiceUrl, username, password);
        RegisterClientHelper registerClientHelper = new RegisterClientHelper(configServiceClient, artifactId, clientName, clientId);

        ProcessExecutorFetcher processExecutorFetcher = new ProcessExecutorFetcher();
        LastRunningProcessFileUtil fileUtil = new LastRunningProcessFileUtil(
                DuplicateProcessHandler.RUNNING_PROCESS_FILENAME);
        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(processExecutorFetcher, fileUtil);

        String workingDirectory = "./";
        ApplicationProcess processHolder = new ApplicationProcess(duplicateProcessHandler);
        processHolder.setWorkingDirectory(new File(workingDirectory));

        EventExtractorService extractorService = new EventExtractorService(new EventRepo(), startPattern);

        new JavaAutoUpdater(configServiceClient, registerClientHelper, processHolder, duplicateProcessHandler,
                extractorService)
                .start(updateInterval, isRunningInterval);
    }
}