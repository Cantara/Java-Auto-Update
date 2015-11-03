package no.cantara.jau.processkill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Makes sure any running managed service is killed if JAU should restart
 */
public class DuplicateProcessHandler {
    private static final Logger log = LoggerFactory.getLogger(DuplicateProcessHandler.class);
    public static final String RUNNING_PROCESS_FILENAME = "last-running-process.txt";
    private ProcessAdapter processAdapter;

    public DuplicateProcessHandler(ProcessExecutorFetcher processExecutorFetcher) {
        this.processAdapter = new ProcessAdapter(processExecutorFetcher);
    }

    public boolean killExistingProcessIfRunning() {
        String pid = getRunningProcessPidFromFile();
        if (pid != null) {
            if (isValidPid(pid)) {
                if (processAdapter.processIsRunning(pid)) {
                    log.info("Last recorded running managed process pid={} is running", pid);
                    return processAdapter.killRunningProcess(pid);
                }
            }
        }
        else {
            log.info("{} not found. Assuming no existing managed process is running.", RUNNING_PROCESS_FILENAME);
        }
        return false;
    }

    public void findRunningManagedProcessPidAndWriteToFile(Process managedProcess) {
        String pid = processAdapter.findProcessId(managedProcess);
        if (pid != null) {
            writePidToFile(pid);
        }
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
            log.error("Could not write to file '{}'", RUNNING_PROCESS_FILENAME, e);
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


}