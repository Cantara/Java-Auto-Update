package no.cantara.jau;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Makes sure any running managed service is killed if JAU should restart
 */
public class DuplicateProcessHandler {
    private static final Logger log = LoggerFactory.getLogger(DuplicateProcessHandler.class);
    public static final String RUNNING_PROCESS_FILENAME = "last-running-process.txt";

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

    private static String findWindowsProcessId(Process process) {
        if (process.getClass().getName().equals("java.lang.Win32Process")
                || process.getClass().getName().equals("java.lang.ProcessImpl")) {
            try {
                Field f = process.getClass().getDeclaredField("handle");
                f.setAccessible(true);
                long handleNumber = f.getLong(process);

                Kernel32 kernel = Kernel32.INSTANCE;
                WinNT.HANDLE handle = new WinNT.HANDLE();
                handle.setPointer(Pointer.createConstant(handleNumber));
                int pid = kernel.GetProcessId(handle);
                log.debug("Found pid for managed process: {}", pid);
                return pid + "";
            } catch (Throwable e) {
                log.error("Could not get PID of managed process. This could lead to duplicate managed processes!",
                        e);
            }
        }
        return null;
    }

    public static void findRunningManagedProcessPidAndWriteToFile(Process managedProcess) {
        String pid = null;
        if (isWindows()) {
            pid = findWindowsProcessId(managedProcess);
        }
        else {
            pid = findUnixProcessId(managedProcess);
        }
        if (pid != null) {
            writePidToFile(pid);
        }
    }

    private static String findUnixProcessId(Process managedProcess) {
        String pid;
        Field pidField = null;
        try {
            pidField = managedProcess.getClass().getDeclaredField("pid");
        } catch (NoSuchFieldException e) {
            log.error("Could not get PID of managed process. This could lead to duplicate managed processes!",
                    e);
            return null;
        }
        try {
            pidField.setAccessible(true);
            pid = Long.toString(pidField.getLong(managedProcess));
        } catch (IllegalAccessException e) {
            log.error("Could not get PID of managed process. This could lead to duplicate managed processes!",
                    e);
            return null;
        }
        return pid;
    }

    private static void writePidToFile(String pid) {
        Path filePath = Paths.get(RUNNING_PROCESS_FILENAME);
        try {
            if (!Files.exists(filePath)) {
                Files.createFile(Paths.get(RUNNING_PROCESS_FILENAME));
            }
        } catch (IOException e) {
           log.error("Could not create file to managed process pid={}", pid, e);
            return;
        }

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(RUNNING_PROCESS_FILENAME), "utf-8"))) {
            writer.write(pid);
            log.debug("Wrote pid={} to file={}", pid, RUNNING_PROCESS_FILENAME);
        } catch (FileNotFoundException e) {
            log.error("File '{}' to write managed process pid={} not found", RUNNING_PROCESS_FILENAME,pid, e);
        } catch (UnsupportedEncodingException e) {
            log.error("Encoding error while writing to {}", RUNNING_PROCESS_FILENAME, e);
        } catch (IOException e) {
            log.error("Could not write to file '{}'", RUNNING_PROCESS_FILENAME);
        }
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

    private static boolean processIsRunning(String pid) {
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
        // TODO: Is this check of valid PID too naive?
        try {
            Long.parseLong(pid);
            return true;
        } catch (NumberFormatException e) {
            log.warn("PID is not valid number. Got: '{}' {}", pid, e);
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

    private static boolean executeProcess(String pid, ProcessBuilder processBuilder) {
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