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
    private static Process currentProcess;

    @Test
    public void shouldKillExistingProcessWhenExistingProcessIsRunning() throws IOException, NoSuchFieldException, IllegalAccessException, InterruptedException {
        createDummyProcess();
        createFileAndWriteLine(PID + "");

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
    public void shouldWritePIDToFile() throws IOException, NoSuchFieldException, IllegalAccessException {
        createDummyProcess();
        DuplicateProcessHandler.findRunningManagedProcessPidAndWriteToFile(currentProcess);

        String pid = new String(Files.readAllBytes(Paths.get(TEST_RUNNING_PROCESS_FILE)));

        Assert.assertEquals(pid, Integer.toString(PID));
    }

    @Test
    public void shouldEraseEarlierContentFromFileWhenWritingPIDToFile() throws IOException, NoSuchFieldException, IllegalAccessException {
        createDummyProcess();
        int firstPid = PID;
        createDummyProcess();
        int secondPid = PID;
        DuplicateProcessHandler.findRunningManagedProcessPidAndWriteToFile(currentProcess);

        String pid = new String(Files.readAllBytes(Paths.get(TEST_RUNNING_PROCESS_FILE)));

        Assert.assertEquals(pid, Integer.toString(secondPid));
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
        currentProcess = Runtime.getRuntime().exec("sleep 4");
        Field f = currentProcess.getClass().getDeclaredField("pid");
        f.setAccessible(true);
        PID = f.getInt(currentProcess);
        log.trace("Created dummy 'sleep 4' process with pid={} ", f.getInt(currentProcess));
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

    @AfterMethod
    private void resetFields() {
        PID = 12349876;
        currentProcess = null;
    }
}
