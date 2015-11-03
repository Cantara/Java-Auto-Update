package no.cantara.jau.processkill;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

public class WindowsProcessExecutor implements ProcessExecutor {
    private static final Logger log = LoggerFactory.getLogger(WindowsProcessExecutor.class);

    @Override
    public ProcessBuilder createKillProcessCommand(String pid) {
        ProcessBuilder processBuilder = new ProcessBuilder("taskkill", "/pid", pid, "/f");
        return processBuilder;
    }

    @Override
    public ProcessBuilder createProcessIsRunningCommand(String pid) {
        ProcessBuilder processBuilder;
        processBuilder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", "tasklist",
                    "/FI", "\"PID eq " + pid + "\" | findstr " + pid + "\"");
        return processBuilder;
    }

    @Override
    public String findProcessId(Process process) throws NoSuchFieldException, IllegalAccessException {
        if (process.getClass().getName().equals("java.lang.Win32Process")
                || process.getClass().getName().equals("java.lang.ProcessImpl")) {
                Field f = process.getClass().getDeclaredField("handle");
                f.setAccessible(true);
                long handleNumber = f.getLong(process);

                Kernel32 kernel = Kernel32.INSTANCE;
                WinNT.HANDLE handle = new WinNT.HANDLE();
                handle.setPointer(Pointer.createConstant(handleNumber));
                int pid = kernel.GetProcessId(handle);
                log.debug("Found pid for managed process: {}", pid);
                return pid + "";
        }
        return null;
    }
}
