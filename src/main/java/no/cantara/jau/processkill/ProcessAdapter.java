package no.cantara.jau.processkill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessAdapter {
    private static final Logger log = LoggerFactory.getLogger(ProcessAdapter.class);
    private ProcessExecutor processExecutor;

    public ProcessAdapter(ProcessExecutorFetcher processExecutorFetcher) {
        this.processExecutor = processExecutorFetcher.getProcessExecutorBasedOnOs();
    }

    public boolean killRunningProcess(String pid) {
        log.info("Killing existing managed process with pid={}", pid);
        ProcessBuilder processBuilder = processExecutor.createKillProcessCommand(pid);

        boolean processWasKilled = executeProcess(processBuilder);
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

    public String findProcessId(Process process) {
        String pid = null;
        try {
            pid = processExecutor.findProcessId(process);
        } catch (ReflectiveOperationException e) {
            log.warn("Finding PID of managed process failed", e);
        }
        return pid;
    }

    public boolean processIsRunning(String pid) {
        ProcessBuilder processBuilder = processExecutor.createProcessIsRunningCommand(pid);
        boolean processIsRunning = executeProcess(processBuilder);
        if (processIsRunning) {
            log.info("Found pid={} of last recorded running managed process", pid);
            return true;
        }
        else {
            log.info("Last recorded running managed process pid={} is not running", pid);
            return false;
        }
    }

    private static boolean executeProcess(ProcessBuilder processBuilder) {
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
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        String result = builder.toString();
        if (!result.isEmpty()) {
            log.error("Error output from kill command: '{}'", result);
        }
    }
}
