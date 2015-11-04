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
    private LastRunningProcessFileUtil fileUtil;

    public DuplicateProcessHandler(ProcessAdapter processAdapter, LastRunningProcessFileUtil fileUtil) {
        this.processAdapter = processAdapter;
        this.fileUtil = fileUtil;
    }

    public boolean killExistingProcessIfRunning() {
        String pid = null;
        try {
            pid = fileUtil.getRunningProcessPidFromFile();
        } catch (IOException e) {
            log.warn("Could not read file={}.", fileUtil.getFileName());
        }
        if (pid != null) {
            if (isValidPid(pid)) {
                if (processAdapter.processIsRunning(pid)) {
                    log.info("Last recorded running managed process pid={} is running", pid);
                    return processAdapter.killRunningProcess(pid);
                }
            }
        }
        else {
            log.info("{} not found. Assuming no existing managed process is running.", fileUtil.getFileName());
        }
        return false;
    }

    public void findRunningManagedProcessPidAndWriteToFile(Process managedProcess) {
        String pid = processAdapter.findProcessId(managedProcess);
        if (pid != null) {
            fileUtil.writePidToFile(pid);
        }
        else {
            log.error("Did not find process id of running managed process!");
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


}