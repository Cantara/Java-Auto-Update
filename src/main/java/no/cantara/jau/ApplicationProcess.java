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

    public void reStartProcess() {
        if (runningProcess != null) {
            if (runningProcess.isAlive()) {
                log.debug("Destroying running process");
                runningProcess.destroy();
                try {
                    runningProcess.waitFor();
                    log.debug("Successfully destroyed running process");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                log.debug("Process already exited with status {}.", runningProcess.exitValue());
            }
        }
        ProcessBuilder pb = new ProcessBuilder(command).inheritIO().directory(workingDirectory);
        try {
            runningProcess = pb.start();
        } catch (IOException e) {
            throw new RuntimeException("", e);
        }
    }

}
