package no.cantara.jau.processkill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public abstract class ProcessExecutor {
    private static final Logger log = LoggerFactory.getLogger(ProcessExecutor.class);

    abstract public boolean killProcessByPID(String pid) throws IOException, InterruptedException;

    public abstract boolean killProcessByProcessName(String processName) throws IOException, InterruptedException;

    abstract public boolean isProcessRunning(String pid) throws IOException, InterruptedException;

    abstract public String findProcessId(Process process) throws ReflectiveOperationException;

    static boolean executeProcess(ProcessBuilder processBuilder) throws IOException, InterruptedException {
        Process p = processBuilder.start();
        p.waitFor();
        if (p.exitValue() == 0) {
            return true;
        } else {
            printErrorCommandFromProcess(p);
        }
        return false;
    }

    static void printErrorCommandFromProcess(Process p) throws IOException {
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
