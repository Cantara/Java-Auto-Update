package no.cantara.jau.processkill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;

public class UnixProcessExecutor extends ProcessExecutor {
    private static final Logger log = LoggerFactory.getLogger(UnixProcessExecutor.class);

    @Override
    public void killProcess(String pid) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("kill", "-9", pid);
        executeProcess(processBuilder);
    }

    @Override
    public boolean isProcessRunning(String pid) throws IOException, InterruptedException {
        ProcessBuilder processBuilder;
        processBuilder = new ProcessBuilder("ps", "-p", pid);
        return executeProcess(processBuilder);
    }

    @Override
    public String findProcessId(Process process) throws NoSuchFieldException, IllegalAccessException {
        String pid;
        Field pidField;
            pidField = process.getClass().getDeclaredField("pid");
            pidField.setAccessible(true);
            pid = Long.toString(pidField.getLong(process));
        return pid;
    }
}
