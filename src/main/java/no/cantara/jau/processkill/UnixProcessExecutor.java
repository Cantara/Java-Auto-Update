package no.cantara.jau.processkill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class UnixProcessExecutor implements ProcessExecutor {
    private static final Logger log = LoggerFactory.getLogger(UnixProcessExecutor.class);

    @Override
    public ProcessBuilder createKillProcessCommand(String pid) {
        ProcessBuilder processBuilder = new ProcessBuilder("kill", "-9", pid);
        return processBuilder;
    }

    @Override
    public ProcessBuilder createProcessIsRunningCommand(String pid) {
        ProcessBuilder processBuilder;
        processBuilder = new ProcessBuilder("ps", "-p", pid);
        return processBuilder;
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
