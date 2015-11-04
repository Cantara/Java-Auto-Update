package no.cantara.jau;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.cantara.jau.processkill.DuplicateProcessHandler;
import no.cantara.jau.serviceconfig.client.ConfigurationStoreUtil;
import no.cantara.jau.serviceconfig.client.DownloadUtil;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

import java.io.File;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import static java.util.concurrent.TimeUnit.MILLISECONDS;



public class JAUProcessTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    ScheduledFuture<?> restarterHandle;
    private ApplicationProcess processHolder;
    private static final Logger log = LoggerFactory.getLogger(JAUProcessTest.class);




    @BeforeClass
    public void startServer() throws InterruptedException {
        DuplicateProcessHandler duplicateProcessHandler = mock(DuplicateProcessHandler.class);
        processHolder = new ApplicationProcess(duplicateProcessHandler);

    }


    @AfterClass
    public void stop() {
        if (processHolder != null) {
            processHolder.stopProcess();
        }
        restarterHandle.cancel(true);

    }


    @Test(enabled=false)
    public void testProcessDownloadStartupAndRunning() throws Exception {


        String jsonResponse = new Scanner( new File("config1.serviceconfig") ).useDelimiter("\\A").next();

        // let us type a configuration the quick way..
        ServiceConfig serviceConfig = mapper.readValue(jsonResponse, ServiceConfig.class);

        // Process stuff
        DuplicateProcessHandler duplicateProcessHandler = mock(DuplicateProcessHandler.class);
        ApplicationProcess processHolder= new ApplicationProcess(duplicateProcessHandler);
        processHolder.setWorkingDirectory(new File("./"));
        String workingDirectory = processHolder.getWorkingDirectory().getAbsolutePath();


        // Download stuff
        DownloadUtil.downloadAllFiles(serviceConfig.getDownloadItems(), workingDirectory);
        ConfigurationStoreUtil.toFiles(serviceConfig.getConfigurationStores(), workingDirectory);

        // Lets try to start
        String initialCommand = serviceConfig.getStartServiceScript();
        int updateInterval=100;

        System.out.println("Initial command: "+initialCommand);
        processHolder.setWorkingDirectory(new File(workingDirectory));
        processHolder.setCommand(initialCommand.split("\\s+"));

        processHolder.startProcess();

        restarterHandle = scheduler.scheduleAtFixedRate(
                () -> {

                    try {
                        // Restart, whatever the reason the process is not running.
                        if (!processHolder.processIsRunning()) {
                            log.info("Process is not running - restarting... clientId={}, lastChanged={}, command={}",
                                    processHolder.getClientId(), processHolder.getLastChangedTimestamp(), processHolder.getCommand());
                            processHolder.startProcess();
                        }
                    } catch (Exception e) {
                        log.warn("Error thrown from scheduled lambda.", e);
                    }
                },
                1, updateInterval, MILLISECONDS
        );


        Thread.sleep(4000);
        assertTrue(processHolder.processIsRunning(), "First check");
        Thread.sleep(1000);
        assertTrue(processHolder.processIsRunning(), "Second check");

        processHolder.stopProcess();
        assertFalse(processHolder.processIsRunning(), "Seventh check");
        Thread.sleep(4000);
        assertTrue(processHolder.processIsRunning(), "Eigth check");

    }

    private static String getStringProperty(final Properties properties, String propertyKey, String defaultValue) {
        String property = properties.getProperty(propertyKey, defaultValue);
        if (property == null) {
            //-Dconfigservice.url=
            property = System.getProperty(propertyKey);
        }
        return property;
    }


}