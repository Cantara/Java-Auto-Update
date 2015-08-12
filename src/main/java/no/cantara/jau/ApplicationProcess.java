package no.cantara.jau;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper of process and related data,
 * primarily used to circumvent Java 8's "final" restriction on closures.
 */
public class ApplicationProcess {
    private static final Logger log = LoggerFactory.getLogger(ApplicationProcess.class);
    private File workingDirectory;
    private String[] command;
    private Process runningProcess;
    private String lastChangedTimestamp;

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public String[] getCommand() {
        return command;
    }

    public void setCommand(String[] command) {
        this.command = command;
    }

    public String getLastChangedTimestamp() {
        return lastChangedTimestamp;
    }

    public void setLastChangedTimestamp(String lastChangedTimestamp) {
        this.lastChangedTimestamp = lastChangedTimestamp;
    }

    public boolean processIsrunning() {
        return runningProcess != null && runningProcess.isAlive();
    }

    public void startProcess() {
        ProcessBuilder pb = new ProcessBuilder(command).inheritIO().directory(workingDirectory);
        try {
            runningProcess = pb.start();
        } catch (IOException e) {
            throw new RuntimeException("IOException while trying to start process with command '" + String.join(" ", command) + "' from directory '" + workingDirectory + "'.", e);
        }
    }

    public void stopProcess() {
        log.debug("Destroying running process");
        if (!processIsrunning()) {
            log.debug("Tried to stop process, but no process was running.");
            return;
        }
        runningProcess.destroy();
        try {
            runningProcess.waitFor();
            log.debug("Successfully destroyed running process");
        } catch (InterruptedException e) {
            log.debug("Interrupted while waiting for process to shut down.", e);
        }
    }

}
