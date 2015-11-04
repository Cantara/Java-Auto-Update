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
    private ProcessExecutorFetcher processExecutorFetcher;
    private ProcessExecutor processExecutor;

    public DuplicateProcessHandler(ProcessAdapter processAdapter, LastRunningProcessFileUtil fileUtil) {
        this.processAdapter = processAdapter;
        this.fileUtil = fileUtil;
        processExecutor = new ProcessExecutorFetcher().getProcessExecutorBasedOnOs();
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
                if (processIsRunning(pid)) {
                    log.info("Last recorded running managed process pid={} is running", pid);
                    return killRunningProcess(pid);
                }
            }
        }
        else {
            log.info("{} not found. Assuming no existing managed process is running.", fileUtil.getFileName());
        }
        return false;
    }

    private boolean processIsRunning(String pid) {
        try {
            boolean processRuns = processExecutor.isProcessRunning(pid);
            if (processRuns) {
                log.info("Last recorded managed process pid={} is running", pid);
            }
            else {
                log.info("Last recorded managed process pid={} is running", pid);
            }
            return processRuns;
        } catch (IOException e) {
            log.error("Exception executing process. Could not check if process with pid={} is running", pid, e);
        } catch (InterruptedException e) {
            log.error("Process interrupted. Could not check if process with pid={} is running", pid, e);
        }
        return false;
    }

    private boolean killRunningProcess(String pid) {
        try {
            processExecutor.killProcess(pid);
            log.info("Successfully killed existing running managed process pid={}", pid);
            return true;
        } catch (IOException e) {
            log.error("Exception executing kill process. Could not kill running managed process pid={}", pid);

        } catch (InterruptedException e) {
            log.error("Kill process was interrupted. Could not kill running managed process pid={}", pid);
        }
        return false;
    }

    public void findRunningManagedProcessPidAndWriteToFile(Process managedProcess) {
        String pid = findProcessId(managedProcess);
        if (pid != null) {
            fileUtil.writePidToFile(pid);
        }
        else {
            log.error("Did not find process id of running managed process!");
        }
    }

    private String findProcessId(Process managedProcess) {
        try {
            return processExecutor.findProcessId(managedProcess);
        } catch (ReflectiveOperationException e) {
            log.error("Could not find pid of managed process");
        }
        return null;
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