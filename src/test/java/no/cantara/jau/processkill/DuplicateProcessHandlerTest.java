package no.cantara.jau.processkill;

import no.cantara.jau.DuplicateProcessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DuplicateProcessHandlerTest {
    private static final Logger log = LoggerFactory.getLogger(DuplicateProcessHandlerTest.class);
    public static final String TEST_RUNNING_PROCESS_FILE = DuplicateProcessHandler.RUNNING_PROCESS_FILENAME;
    private static int PID = 12349876;

    @Test
    public void shouldKillExistingProcessWhenExistingProcessIsRunning() throws IOException, NoSuchFieldException, IllegalAccessException, InterruptedException {
        createDummyProcess();

        boolean processWasKilled = DuplicateProcessHandler.killExistingProcessIfRunning();
        boolean processIsRunning = checkIfProcessIsRunning();

        Assert.assertEquals(processWasKilled, !processIsRunning);
    }

    @Test
    public void shouldFailToKillExistingProcessWhenExistingProcessIsNotRunning() throws IOException, InterruptedException {
        boolean processWasKilled = DuplicateProcessHandler.killExistingProcessIfRunning();
        boolean processIsRunning = checkIfProcessIsRunning();

        Assert.assertEquals(processWasKilled, processIsRunning);
    }

    @Test
    public void shouldNotKillProcessWhenPidIsNotValid() throws IOException, InterruptedException {
        createFileAndWriteLine("notvalidpid");

        boolean processWasKilled = DuplicateProcessHandler.killExistingProcessIfRunning();
        boolean processIsRunning = checkIfProcessIsRunning();

        Assert.assertEquals(processWasKilled, processIsRunning);
    }

    @Test
    public void shouldHandleIfRunningProcessFileDoesNotExist() {
        boolean processWasKilled = DuplicateProcessHandler.killExistingProcessIfRunning();

        Assert.assertFalse(processWasKilled);
    }

    @Test
    public void shouldWritePIDToFile() throws IOException {
        DuplicateProcessHandler.writeRunningManagedProcessPidToFile("123345");

        String pid = new String(Files.readAllBytes(Paths.get(TEST_RUNNING_PROCESS_FILE)));

        Assert.assertEquals(pid, "123345");
    }

    @Test
    public void shouldEraseEarlierContentFromFileWhenWritingPIDToFile() throws IOException {
        String firstPid = "12345";
        String secondPid = "34567";
        DuplicateProcessHandler.writeRunningManagedProcessPidToFile(firstPid);
        DuplicateProcessHandler.writeRunningManagedProcessPidToFile(secondPid);

        String pid = new String(Files.readAllBytes(Paths.get(TEST_RUNNING_PROCESS_FILE)));

        Assert.assertEquals(pid, secondPid);
    }

    private static boolean checkIfProcessIsRunning() throws IOException, InterruptedException {
        ProcessBuilder processBuilder;
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            processBuilder = new ProcessBuilder("taskkill", Integer.toString(PID));
        } else { //unix
            processBuilder = new ProcessBuilder("kill", "-9", Integer.toString(PID));
        }

        Process p = processBuilder.start();
        p.waitFor();
        if (p.exitValue() == 0) {
            return true;
        }
        return false;
    }

    private void createDummyProcess() throws IOException, NoSuchFieldException, IllegalAccessException {
        Process p = Runtime.getRuntime().exec("vi");
        Field f = p.getClass().getDeclaredField("pid");
        f.setAccessible(true);
        PID = f.getInt(p);
        log.trace("Created dummy 'vi' process with pid={} ", f.getInt(p));
        createFileAndWriteLine(PID + "");
    }

    private void createFileAndWriteLine(String lineToWrite) throws IOException {
        Files.createFile(Paths.get(TEST_RUNNING_PROCESS_FILE));

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(TEST_RUNNING_PROCESS_FILE), "utf-8"))) {
            writer.write(lineToWrite);
        }

        log.debug("Created test file " + TEST_RUNNING_PROCESS_FILE);
    }

    @AfterMethod
    private void deleteTestRunningProcessFile() throws IOException {
        Files.deleteIfExists(Paths.get(TEST_RUNNING_PROCESS_FILE));
        log.debug("Deleted test file (if existed) " + TEST_RUNNING_PROCESS_FILE);
    }
}
