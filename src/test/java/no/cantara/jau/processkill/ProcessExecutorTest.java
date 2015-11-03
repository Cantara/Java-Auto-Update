package no.cantara.jau.processkill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ProcessExecutorTest {
    private static final Logger log = LoggerFactory.getLogger(ProcessExecutorTest.class);

    @Test
    public void shouldKillExistingProcessWhenExistingProcessIsRunning() throws IOException,
            NoSuchFieldException, IllegalAccessException, InterruptedException {
        String fileName = "killExistingProcessWhenRunningTest.txt";
        LastRunningProcessFileUtil fileUtil = new LastRunningProcessFileUtil(fileName);
        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(new ProcessAdapter(
                new ProcessExecutorFetcher()), fileUtil);
        Process p = createDummyProcess();
        int PID = getPIDFromProcess(p);
        createFileAndWriteLine(fileName, PID + "");

        boolean processWasKilled = duplicateProcessHandler.killExistingProcessIfRunning();
        boolean processIsRunning = checkIfProcessIsRunning(PID);

        Assert.assertEquals(processWasKilled, !processIsRunning);

        deleteTestRunningProcessFile(fileName);
    }

    @Test
    public void shouldFailToKillExistingProcessWhenExistingProcessIsNotRunning() throws IOException, InterruptedException {
        String fileName = "shouldFailToKillExistingProcessTest.txt";
        LastRunningProcessFileUtil fileUtil = new LastRunningProcessFileUtil(fileName);
        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(new ProcessAdapter(
                new ProcessExecutorFetcher()), fileUtil);
        int PID = 987654;

        boolean processWasKilled = duplicateProcessHandler.killExistingProcessIfRunning();
        boolean processIsRunning = checkIfProcessIsRunning(PID);

        Assert.assertEquals(processWasKilled, processIsRunning);
    }

    @Test
    public void shouldNotKillProcessWhenPidIsNotValid() throws IOException, InterruptedException {
        String fileName = "pidNotValidTest.txt";
        LastRunningProcessFileUtil fileUtil = new LastRunningProcessFileUtil(fileName);
        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(new ProcessAdapter(
                new ProcessExecutorFetcher()), fileUtil);
        createFileAndWriteLine(fileName, "notvalidpid");

        boolean processWasKilled = duplicateProcessHandler.killExistingProcessIfRunning();
        //boolean processIsRunning = checkIfProcessIsRunning(PID);

        Assert.assertFalse(processWasKilled);

        deleteTestRunningProcessFile(fileName);
    }

    @Test
    public void shouldHandleIfRunningProcessFileDoesNotExist() {
        String fileName = "nonExistantFileTest.txt";
        LastRunningProcessFileUtil fileUtil = new LastRunningProcessFileUtil(fileName);
        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(new ProcessAdapter(
                new ProcessExecutorFetcher()), fileUtil);

        boolean processWasKilled = duplicateProcessHandler.killExistingProcessIfRunning();

        Assert.assertFalse(processWasKilled);
    }

    @Test
    public void shouldWritePIDToFile() throws IOException, NoSuchFieldException, IllegalAccessException {
        String fileName = "shouldWritePidToFileTest.txt";
        LastRunningProcessFileUtil fileUtil = new LastRunningProcessFileUtil(fileName);
        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(new ProcessAdapter(
                new ProcessExecutorFetcher()), fileUtil);
        Process currentProcess = createDummyProcess();
        long PID = getPIDFromProcess(currentProcess);
        duplicateProcessHandler.findRunningManagedProcessPidAndWriteToFile(currentProcess);

        String pid = new String(Files.readAllBytes(Paths.get(fileName)));

        Assert.assertEquals(pid, Long.toString(PID));

        deleteTestRunningProcessFile(fileName);
    }

    @Test
    public void shouldOverwriteFileWhenWritingPIDToFile() throws IOException, NoSuchFieldException, IllegalAccessException {
        String fileName = "shouldOverwriteFileTest.txt";
        LastRunningProcessFileUtil fileUtil = new LastRunningProcessFileUtil(fileName);
        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(new ProcessAdapter(
                new ProcessExecutorFetcher()), fileUtil);
        Process currentProcess = createDummyProcess();
        long firstPid = getPIDFromProcess(currentProcess);
        currentProcess = createDummyProcess();
        long secondPid = getPIDFromProcess(currentProcess);
        duplicateProcessHandler.findRunningManagedProcessPidAndWriteToFile(currentProcess);

        String pid = new String(Files.readAllBytes(Paths.get(fileName)));

        Assert.assertEquals(pid, Long.toString(secondPid));

        deleteTestRunningProcessFile(fileName);
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

    private void createFileAndWriteLine(String fileName, String lineToWrite) throws IOException {
        Files.createFile(Paths.get(fileName));

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(fileName), "utf-8"))) {
            writer.write(lineToWrite);
        }

        log.debug("Created test file " + fileName);
    }


    private void deleteTestRunningProcessFile(String fileName) throws IOException {
        Files.deleteIfExists(Paths.get(fileName));
        log.debug("Deleted test file (if existed) " + fileName);
    }
}
