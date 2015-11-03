package no.cantara.jau.processkill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ProcessAdapter {
    private static final Logger log = LoggerFactory.getLogger(ProcessAdapter.class);
    private ProcessExecutor processExecutor;

    public ProcessAdapter(ProcessExecutorFetcher processExecutorFetcher) {
        this.processExecutor = processExecutorFetcher.getProcessExecutorBasedOnOs();
    }

    public boolean killRunningProcess(String pid) {
        log.info("Killing existing managed process with pid={}", pid);
        try {
            processExecutor.killProcess(pid);
            log.info("Successfully killed existing running managed process pid={}", pid);
            return true;
        } catch (IOException e) {
            log.error("IOException. Could not kill existing running managed process pid={}.",
                    pid, e);
        } catch (InterruptedException e) {
            log.error("InterruptedException. Could not kill existing running managed process pid={}.",
                    pid, e);
        }
        return false;
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
        boolean processIsRunning = false;
        try {
            processIsRunning = processExecutor.isProcessRunning(pid);
        } catch (IOException e) {
            log.error("IOException. Could not check if process with pid={} is running", pid, e);
        } catch (InterruptedException e) {
            log.error("InterruptedException. Could not check if process with pid={} is running", pid, e);
        }
        if (processIsRunning) {
            log.info("Found pid={} of last recorded running managed process", pid);
            return true;
        } else {
            log.info("Last recorded running managed process pid={} is not running", pid);
            return false;
        }
    }
}
