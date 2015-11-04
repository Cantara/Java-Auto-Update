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

    public void killRunningProcess(String pid) throws IOException, InterruptedException {
        processExecutor.killProcess(pid);
    }

    public String findProcessId(Process process) throws ReflectiveOperationException {
        return processExecutor.findProcessId(process);
    }

    public boolean processIsRunning(String pid) throws IOException, InterruptedException {
        return processExecutor.isProcessRunning(pid);
    }
}
