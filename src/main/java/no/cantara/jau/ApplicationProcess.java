package no.cantara.jau;

import no.cantara.jau.duplicatehandler.DuplicateProcessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Wrapper of process and related data,
 * primarily used to circumvent Java 8's "final" restriction on closures.
 */
public class ApplicationProcess {
    private static final Logger log = LoggerFactory.getLogger(ApplicationProcess.class);
    private File workingDirectory;
    private String[] command;
    private Process runningProcess;

    private String clientId;
    private String lastChangedTimestamp;
    private DuplicateProcessHandler duplicateProcessHandler;

    public ApplicationProcess(DuplicateProcessHandler duplicateProcessHandler) {
        this.duplicateProcessHandler = duplicateProcessHandler;
    }

    public boolean processIsRunning() {
        return runningProcess != null && runningProcess.isAlive();
    }

    public void startProcess() {
        ProcessBuilder pb = new ProcessBuilder(command).inheritIO().directory(workingDirectory);
        try {
            runningProcess = pb.start();
            Thread.sleep(1000); // Gives the process time to fail
            if(runningProcess.isAlive()) {
            	duplicateProcessHandler.findRunningManagedProcessPidAndWriteToFile(runningProcess);
            } else {
            	log.warn("Failed to start process with command: {}, workingDirectory: {}, clientId: {}, lastChangedTimestamp: {}", command, workingDirectory, clientId, lastChangedTimestamp);
            }
        } catch (IOException e) {
            throw new RuntimeException("IOException while trying to start process with command '" + String.join(" ", command) + "' from directory '" + workingDirectory + "'.", e);
        } catch (InterruptedException e) {
			log.warn("Not allowed to sleep for 1 second", e);
		}
    }

    public void stopProcess() {
        log.debug("Destroying running process");
        if (!processIsRunning()) {
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

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
    public void setCommand(String[] command) {
        this.command = command;
    }
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    public void setLastChangedTimestamp(String lastChangedTimestamp) {
        this.lastChangedTimestamp = lastChangedTimestamp;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }
    public String[] getCommand() {
        return command;
    }
    public String getClientId() {
        return clientId;
    }
    public String getLastChangedTimestamp() {
        return lastChangedTimestamp;
    }
}
