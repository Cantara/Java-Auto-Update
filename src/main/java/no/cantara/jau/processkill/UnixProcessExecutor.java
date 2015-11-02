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
    public String findProcessId(Process process) {
        String pid;
        Field pidField;
        try {
            pidField = process.getClass().getDeclaredField("pid");
        } catch (NoSuchFieldException e) {
            log.error("Could not get PID of managed process. This could lead to duplicate managed processes!",
                    e);
            return null;
        }
        try {
            pidField.setAccessible(true);
            pid = Long.toString(pidField.getLong(process));
        } catch (IllegalAccessException e) {
            log.error("Could not get PID of managed process. This could lead to duplicate managed processes!",
                    e);
            return null;
        }
        return pid;
    }
}
