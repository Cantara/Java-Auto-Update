package no.cantara.jau.processkill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Makes sure any running managed service is killed if JAU should restart
 */
public class DuplicateProcessHandler {
    private static final Logger log = LoggerFactory.getLogger(DuplicateProcessHandler.class);
    public static final String RUNNING_PROCESS_FILENAME = "last-running-process.txt";
    private LastRunningProcessFileUtil fileUtil;
    private ProcessExecutor processExecutor;

    public DuplicateProcessHandler(ProcessExecutorFetcher processExecutorFetcher, LastRunningProcessFileUtil fileUtil) {
        this.fileUtil = fileUtil;
        processExecutor = processExecutorFetcher.getProcessExecutorBasedOnOs();
    }

    /**
     * Returns true if process is killed or process is not running. Returns false if any error
     */
    public boolean killExistingProcessIfRunning() {
        String pid;
        try {
            pid = fileUtil.getRunningProcessPidFromFile();
        } catch (IOException e) {
            log.warn("Could not read file={}.", fileUtil.getFileName());
            //TODO: fallback to find process by name
            return false;g // STOPSHIP: 05/11/15  
        }
        if (pid != null) {
            if (isValidPid(pid)) {
                return findRunningProcessByPIDAndKill(pid);
            } else {
                //TODO: fallback to find process by name
                return false;
            }
        } else {
            log.info("{} not found. Assuming no existing managed process is running.", fileUtil.getFileName());
            return true;
        }
    }

    private boolean findRunningProcessByPIDAndKill(String pid) {
        try {
            if (processIsRunning(pid)) {
                log.info("Last recorded managed process pid={} is running", pid);
                return killRunningProcess(pid);
            } else {
                log.info("Last recorded managed process pid={} is not running.", pid);
                return true;
            }
        } catch (IOException e) {
            log.error("Exception executing process. Could not check if process with pid={} is running", pid, e);
        } catch (InterruptedException e) {
            log.error("Process interrupted. Could not check if process with pid={} is running", pid, e);
        }
        return false;
    }

    private boolean processIsRunning(String pid) throws IOException, InterruptedException {
        boolean processRuns = processExecutor.isProcessRunning(pid);
        return processRuns;
    }

    private boolean killRunningProcess(String pid) {
        try {
            processExecutor.killProcess(pid);
            log.info("Successfully killed existing running managed process pid={}", pid);
            return true;
        } catch (IOException e) {
            log.error("Exception executing kill process. Could not kill running managed process pid={}", pid, e);

        } catch (InterruptedException e) {
            log.error("Kill process was interrupted. Could not kill running managed process pid={}", pid, e);
        }
        return false;
    }

    public void findRunningManagedProcessPidAndWriteToFile(Process managedProcess) {
        String pid = findProcessId(managedProcess);
        if (pid != null) {
            fileUtil.writePidToFile(pid);
        } else {
            log.error("Did not find process id of running managed process!");
        }
    }

    private String findProcessId(Process managedProcess) {
        try {
            return processExecutor.findProcessId(managedProcess);
        } catch (ReflectiveOperationException e) {
            log.error("Could not find pid of managed process", e);
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