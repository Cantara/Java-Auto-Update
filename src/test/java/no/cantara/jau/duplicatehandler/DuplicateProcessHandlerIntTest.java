package no.cantara.jau.duplicatehandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DuplicateProcessHandlerIntTest {
    private static final Logger log = LoggerFactory.getLogger(DuplicateProcessHandlerIntTest.class);
    private static final String PROCESS_COMMAND = "java -jar pharmacy-agent-0.8-SNAPSHOT.jar";;
    private static final String TEST_PID_FILE = "target/test-pid.txt";

    @BeforeMethod
    public void cleanup() throws IOException {
        deleteTestRunningProcessFile();
    }

    @Test
    public void shouldKillExistingProcessWhenExistingProcessIsRunning() throws IOException,
            NoSuchFieldException, IllegalAccessException, InterruptedException {
        LastRunningProcessFileUtil fileUtil = new LastRunningProcessFileUtil(TEST_PID_FILE);
        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(
                new ProcessExecutorFetcher(), fileUtil);
        Process p = createDummyProcess();
        int PID = getPIDFromProcess(p);
        createFileAndWriteLine(PID + "");

        boolean processWasKilled = duplicateProcessHandler.killExistingProcessIfRunning(PROCESS_COMMAND);
        boolean processIsRunning = checkIfProcessIsRunning(PID);

        Assert.assertEquals(processWasKilled, !processIsRunning);
    }

    //Tries to kill a process with given PID, so disabled for CI tool.
    //Cannot be certain that process with PID does not exists
    @Test(enabled = false)
    public void shouldReturnTrueWhenTryingToKillNonRunningProcess() throws IOException, InterruptedException {
        LastRunningProcessFileUtil fileUtil = new LastRunningProcessFileUtil(TEST_PID_FILE);
        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(
                new ProcessExecutorFetcher(), fileUtil);
        int PID = 987654;

        boolean processWasKilled = duplicateProcessHandler.killExistingProcessIfRunning(PROCESS_COMMAND);
        boolean processIsRunning = checkIfProcessIsRunning(PID);

        Assert.assertTrue(processWasKilled);
        Assert.assertFalse(processIsRunning);
    }

    @Test
    public void shouldNotKillProcessWhenPidIsNotValid() throws IOException, InterruptedException {
        LastRunningProcessFileUtil fileUtil = new LastRunningProcessFileUtil(TEST_PID_FILE);
        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(
                new ProcessExecutorFetcher(), fileUtil);
        createFileAndWriteLine("notvalidpid");

        boolean processWasKilled = duplicateProcessHandler.killExistingProcessIfRunning(PROCESS_COMMAND);

        Assert.assertFalse(processWasKilled);
    }

    @Test
    public void shouldWritePIDToFile() throws IOException, NoSuchFieldException, IllegalAccessException {
        LastRunningProcessFileUtil fileUtil = new LastRunningProcessFileUtil(TEST_PID_FILE);
        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(
                new ProcessExecutorFetcher(), fileUtil);
        Process currentProcess = createDummyProcess();
        long PID = getPIDFromProcess(currentProcess);
        duplicateProcessHandler.findRunningManagedProcessPidAndWriteToFile(currentProcess);

        String pid = new String(Files.readAllBytes(Paths.get(TEST_PID_FILE)));

        Assert.assertEquals(pid, Long.toString(PID));
    }

    @Test
    public void shouldReturnTrueIfRunningProcessFileDoesNotExist() throws IOException {
        LastRunningProcessFileUtil fileUtil = new LastRunningProcessFileUtil(TEST_PID_FILE);
        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(
                new ProcessExecutorFetcher(), fileUtil);

        boolean processWasKilled = duplicateProcessHandler.killExistingProcessIfRunning(PROCESS_COMMAND);

        Assert.assertTrue(processWasKilled);
    }

    @Test
    public void shouldOverwriteFileWhenWritingPIDToFile() throws IOException, NoSuchFieldException, IllegalAccessException {
        LastRunningProcessFileUtil fileUtil = new LastRunningProcessFileUtil(TEST_PID_FILE);
        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(
                new ProcessExecutorFetcher(), fileUtil);
        Process currentProcess = createDummyProcess();
        long firstPid = getPIDFromProcess(currentProcess);
        currentProcess = createDummyProcess();
        long secondPid = getPIDFromProcess(currentProcess);
        duplicateProcessHandler.findRunningManagedProcessPidAndWriteToFile(currentProcess);

        String pid = new String(Files.readAllBytes(Paths.get(TEST_PID_FILE)));

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

    private static Process createDummyProcess() throws IOException, NoSuchFieldException, IllegalAccessException {
        return Runtime.getRuntime().exec("sleep 4");
    }

    private static int getPIDFromProcess(Process process) throws IllegalAccessException, NoSuchFieldException {
        Field f = process.getClass().getDeclaredField("pid");
        f.setAccessible(true);
        int PID = f.getInt(process);
        return PID;
    }

    private static void createFileAndWriteLine(String lineToWrite) throws IOException {
        Files.createFile(Paths.get(TEST_PID_FILE));

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(TEST_PID_FILE), "utf-8"))) {
            writer.write(lineToWrite);
        }
    }


    private static void deleteTestRunningProcessFile() throws IOException {
        Files.deleteIfExists(Paths.get(TEST_PID_FILE));
    }
}
