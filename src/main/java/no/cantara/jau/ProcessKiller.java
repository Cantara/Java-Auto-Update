package no.cantara.jau;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Makes sure any running managed service is killed if JAU should restart
 */
public class ProcessKiller {
    private static final Logger log = LoggerFactory.getLogger(ProcessKiller.class);
    public static final String RUNNING_PROCESS_FILENAME = "runningProcess.txt";

    public static boolean killExistingProcessIfRunning() {
        String pid = getRunningProcessPidFromFile();
        if (pid != null) {
            if (isValidPid(pid)) {
                if (processIsRunning(pid)) {
                    log.info("Last recorded running managed process pid={} is running", pid);
                    return killRunningProcessBasedOnOS(pid);
                }
            }
        }
        else {
            log.info("{} not found. Assuming no existing managed process is running.", RUNNING_PROCESS_FILENAME);
        }
        return false;
    }

    public static void writeRunningManagedProcessPidToFile() {

    }

    public static void removeRunningManagedProcessPidFromFile() {

    }

    private static String getRunningProcessPidFromFile() {
        Path file = Paths.get(RUNNING_PROCESS_FILENAME);
        String pid = null;
        if (Files.exists(file)) {
            try {
                pid = new String(Files.readAllBytes(Paths.get(RUNNING_PROCESS_FILENAME)));
            } catch (IOException e) {
                log.warn("Could not read file={}. Possible multiple processes!", RUNNING_PROCESS_FILENAME);
            }
        }
        return pid;
    }

    public static boolean processIsRunning(String pid) {
        ProcessBuilder processBuilder;
        if (isWindows()) {
            //tasklist exit code is always 0. Parse output
            //findstr exit code 0 if found pid, 1 if it doesn't
            processBuilder = new ProcessBuilder("cmd /c \"tasklist /FI \"PID eq " +
                    pid + "\" | findstr " + pid + "\"");
        }
        else {
            processBuilder = new ProcessBuilder("ps", "-p", pid);
        }
        boolean processIsRunning = executeProcessRunningCheck(pid, processBuilder);
        return processIsRunning;
    }

    private static boolean executeProcessRunningCheck(String pid, ProcessBuilder processBuilder) {
        boolean processIsRunning = executeProcess(pid, processBuilder);
        if (processIsRunning) {
            log.info("Found pid={} of last recorded running managed process", pid);
            return true;
        }
        else {
            log.info("Last recorded running managed process pid={} is not running", pid);
            return false;
        }
    }

    private static boolean isValidPid(String pid) {
        // TODO: How to check if valid pid?
        try {
            Integer.parseInt(pid);
            return true;
        } catch (NumberFormatException e) {
            log.warn("PID is not valid integer. Got: '{}' {}", pid, e);
            return false;
        }
    }

    private static boolean killRunningProcessBasedOnOS(String pid) {
        log.info("Killing existing managed process with pid={}", pid);
        ProcessBuilder processBuilder;

        if (isWindows()) {
            processBuilder = new ProcessBuilder("taskkill", pid);
        } else { //unix
            processBuilder = new ProcessBuilder("kill", "-9", pid);
        }

        boolean processWasKilled = executeProcess(pid, processBuilder);
        if (processWasKilled) {
            log.info("Successfully killed existing running managed process pid={}", pid);
            return true;
        }
        else {
            log.error("Could not kill existing running managed process pid={}. Possible multiple processes!",
                    pid);
            return false;
        }
    }

    public static boolean executeProcess(String pid, ProcessBuilder processBuilder) {
        try {
            Process p = processBuilder.start();
            try {
                p.waitFor();
                if (p.exitValue() == 0) {
                    return true;
                } else {
                    printErrorCommandFromProcess(p);
                }
            } catch (InterruptedException e) {
                log.warn("Interrupted execution of process", e);
            }
        } catch (IOException e) {
            log.warn("IOException with execution of process", e);
        }
        return false;
    }

    private static void printErrorCommandFromProcess(Process p) throws IOException {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(p.getErrorStream()));
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        String result = builder.toString();
        if (!result.isEmpty()) {
            log.error("Error output from kill command: '{}'", result);
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }
}