package no.cantara.jau.processkill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DuplicateProcessHandlerTest {
    private static final Logger log = LoggerFactory.getLogger(DuplicateProcessHandlerTest.class);
    public static final String TEST_RUNNING_PROCESS_FILE = DuplicateProcessHandler.RUNNING_PROCESS_FILENAME;

    @Test
    public void shouldKillExistingProcessWhenExistingProcessIsRunning() throws IOException, NoSuchFieldException, IllegalAccessException, InterruptedException {
        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(new ProcessAdapter());
        Process p = createDummyProcess();
        int PID = getPIDFromProcess(p);
        createFileAndWriteLine(PID + "");

        boolean processWasKilled = duplicateProcessHandler.killExistingProcessIfRunning();
        boolean processIsRunning = checkIfProcessIsRunning(PID);

        Assert.assertEquals(processWasKilled, !processIsRunning);
    }

    @Test
    public void shouldFailToKillExistingProcessWhenExistingProcessIsNotRunning() throws IOException, InterruptedException {
        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(new ProcessAdapter());
        int PID = 987654;

        boolean processWasKilled = duplicateProcessHandler.killExistingProcessIfRunning();
        boolean processIsRunning = checkIfProcessIsRunning(PID);

        Assert.assertEquals(processWasKilled, processIsRunning);
    }

    @Test
    public void shouldNotKillProcessWhenPidIsNotValid() throws IOException, InterruptedException {
        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(new ProcessAdapter());
        createFileAndWriteLine("notvalidpid");

        boolean processWasKilled = duplicateProcessHandler.killExistingProcessIfRunning();
        //boolean processIsRunning = checkIfProcessIsRunning(PID);

        Assert.assertFalse(processWasKilled);
    }

    @Test
    public void shouldHandleIfRunningProcessFileDoesNotExist() {
        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(new ProcessAdapter());

        boolean processWasKilled = duplicateProcessHandler.killExistingProcessIfRunning();

        Assert.assertFalse(processWasKilled);
    }

    @Test
    public void shouldWritePIDToFile() throws IOException, NoSuchFieldException, IllegalAccessException {
        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(new ProcessAdapter());
        Process currentProcess = createDummyProcess();
        long PID = getPIDFromProcess(currentProcess);
        duplicateProcessHandler.findRunningManagedProcessPidAndWriteToFile(currentProcess);

        String pid = new String(Files.readAllBytes(Paths.get(TEST_RUNNING_PROCESS_FILE)));

        Assert.assertEquals(pid, Long.toString(PID));
    }

    @Test
    public void shouldEraseEarlierContentFromFileWhenWritingPIDToFile() throws IOException, NoSuchFieldException, IllegalAccessException {
        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(new ProcessAdapter());
        Process currentProcess = createDummyProcess();
        long firstPid = getPIDFromProcess(currentProcess);
        currentProcess = createDummyProcess();
        long secondPid = getPIDFromProcess(currentProcess);
        duplicateProcessHandler.findRunningManagedProcessPidAndWriteToFile(currentProcess);

        String pid = new String(Files.readAllBytes(Paths.get(TEST_RUNNING_PROCESS_FILE)));

        Assert.assertEquals(pid, Long.toString(secondPid));
    }

    private static boolean checkIfProcessIsRunning(long PID) throws IOException, InterruptedException {
        ProcessBuilder processBuilder;
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            processBuilder = new ProcessBuilder("taskkill", Long.toString(PID));
        } else { //unix
            processBuilder = new ProcessBuilder("kill", "-9", Long.toString(PID));
        }

        Process p = processBuilder.start();
        p.waitFor();
        if (p.exitValue() == 0) {
            return true;
        }
        return false;
    }

    private Process createDummyProcess() throws IOException, NoSuchFieldException, IllegalAccessException {
        Process currentProcess = Runtime.getRuntime().exec("sleep 4");
        return currentProcess;
    }

    private int getPIDFromProcess(Process process) throws IllegalAccessException, NoSuchFieldException {
        Field f = process.getClass().getDeclaredField("pid");
        f.setAccessible(true);
        int PID = f.getInt(process);
        return PID;
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
