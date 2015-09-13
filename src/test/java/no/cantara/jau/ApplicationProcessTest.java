package no.cantara.jau;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Created by totto on 13.09.15.
 */
public class ApplicationProcessTest {

    private ApplicationProcess processHolder;
    private static final Logger log = LoggerFactory.getLogger(ApplicationProcessTest.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);



    @BeforeClass
    public void startServer() throws InterruptedException {
        processHolder = new ApplicationProcess();
        processHolder.setWorkingDirectory(new File("./"));
        String[] command = new String[2];
        command[0] = "sleep";
        command[1] = "10";
        int updateInterval=100;
        processHolder.setCommand(command);
        processHolder.startProcess();

        final ScheduledFuture<?> restarterHandle = scheduler.scheduleAtFixedRate(
                () -> {

                    try {
                        // Restart, whatever the reason the process is not running.
                        if (!processHolder.processIsrunning()) {
                            log.debug("Process is not running - restarting... clientId={}, lastChanged={}, command={}",
                                    processHolder.getClientId(), processHolder.getLastChangedTimestamp(), processHolder.getCommand());
                            processHolder.startProcess();
                        }
                    } catch (Exception e) {
                        log.debug("Error thrown from scheduled lambda.", e);
                    }
                },
                1, updateInterval, MILLISECONDS
        );

    }

    @AfterClass
    public void stop() {
        processHolder.startProcess();
    }


    @Test
    public void testProcessRunning() throws Exception {
        Thread.sleep(4000);
        assertTrue(processHolder.processIsrunning(), "First check");
        Thread.sleep(1000);
        assertTrue(processHolder.processIsrunning(), "Second check");
        Thread.sleep(1000);
        assertTrue(processHolder.processIsrunning(), "Third check");
        Thread.sleep(1000);
        assertTrue(processHolder.processIsrunning(), "Fourth check");
        processHolder.stopProcess();
        assertFalse(processHolder.processIsrunning(), "Fifth check");
        Thread.sleep(4000);
        assertTrue(processHolder.processIsrunning(), "Sixt check");
        processHolder.stopProcess();
        assertFalse(processHolder.processIsrunning(), "Seventh check");
        Thread.sleep(4000);
        assertTrue(processHolder.processIsrunning(), "Eigth check");


    }

}
