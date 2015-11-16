package no.cantara.jau.duplicatehandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * @param processCommand The command used to launch the application, e.g. "java -jar example-application.jar"
     */
    public boolean killExistingProcessIfRunning(String processCommand) {
        String pid;
        try {
            pid = fileUtil.getRunningProcessPidFromFile();
        } catch (IOException e) {
            log.warn("Could not read file={}.", fileUtil.getFileName());
            return findRunningProcessByProcessNameAndKill(processCommand);
        }
        if (pid != null) {
            if (isValidPid(pid) && findRunningProcessByPIDAndKill(pid)) {
                return true;
            } else {
                return findRunningProcessByProcessNameAndKill(processCommand);
            }
        } else {
            log.info("{} not found. Assuming no existing managed process is running.", fileUtil.getFileName());
            return true;
        }
    }

    public void findRunningManagedProcessPidAndWriteToFile(Process managedProcess) {
        String pid = findProcessId(managedProcess);
        if (pid != null) {
            fileUtil.writePidToFile(pid);
        } else {
            log.error("Did not find process id of running managed process!");
        }
    }

    private boolean findRunningProcessByPIDAndKill(String pid) {
        try {
            if (processIsRunning(pid)) {
                log.info("Last recorded managed process pid={} is running", pid);
                return killRunningProcessByPID(pid);
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

    private boolean findRunningProcessByProcessNameAndKill(String processCommand) {
        String processName = extractProcessNameFromCommand(processCommand);

        if (processName != null) {
            return killRunningProcessByProcessName(processName);
        }

        return false;
    }

    private boolean killRunningProcessByProcessName(String processName) {
        try {
            return processExecutor.killProcessByProcessName(processName);
        } catch (IOException e) {
            log.error("Could not kill process by process name", e);
        } catch (InterruptedException e) {
            log.error("Could not kill process by process name", e);
        }
        return false;
    }

    public String extractProcessNameFromCommand(String processCommand) {
        String regex = "^\\S+\\.[A-Za-z]{3}$";
        String[] wordsInProcessCommand = processCommand.split(" ");
        Pattern pattern = Pattern.compile(regex);
        for (String word : wordsInProcessCommand) {
            Matcher matcher = pattern.matcher(word);
            if (matcher.find())
            {
                String fileName = matcher.group(0);
                log.debug("Extracted name={} of old running process from command={}", fileName, processCommand);
                return fileName;
            }
        }
        log.error("No matching name of old running process found in command={} using regex={}", processCommand, regex);
        return null;
    }

    private boolean processIsRunning(String pid) throws IOException, InterruptedException {
        boolean processRuns = processExecutor.isProcessRunning(pid);
        return processRuns;
    }

    private boolean killRunningProcessByPID(String pid) {
        try {
            boolean processWasKilled = processExecutor.killProcessByPID(pid);
            if (processWasKilled) {
                log.info("Successfully killed existing running managed process pid={}", pid);
                return true;
            }
        } catch (IOException e) {
            log.error("Exception executing kill process. Could not kill running managed process pid={}", pid, e);

        } catch (InterruptedException e) {
            log.error("Kill process was interrupted. Could not kill running managed process pid={}", pid, e);
        }
        return false;
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