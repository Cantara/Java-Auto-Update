package no.cantara.jau.processkill;

public interface ProcessExecutor {
    ProcessBuilder createKillProcessCommand(String pid);

    ProcessBuilder createProcessIsRunningCommand(String pid);

    String findProcessId(Process process) throws ReflectiveOperationException;
}
