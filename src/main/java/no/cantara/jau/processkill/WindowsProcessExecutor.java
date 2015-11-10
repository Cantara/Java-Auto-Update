package no.cantara.jau.processkill;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;

public class WindowsProcessExecutor extends ProcessExecutor {
    private static final Logger log = LoggerFactory.getLogger(WindowsProcessExecutor.class);

    @Override
    public boolean killProcessByPID(String pid) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("taskkill", "/pid", pid, "/f");
        return executeProcess(processBuilder);
    }

    @Override
    public boolean killProcessByProcessName(String processName) throws IOException, InterruptedException {
        //TASKKILL /F /FI "PID ne 1000” /IM java.exe
        return false;
    }

    @Override
    public boolean isProcessRunning(String pid) throws IOException, InterruptedException {
        ProcessBuilder processBuilder;
        processBuilder = new ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", "tasklist",
                "/FI", "\"PID eq " + pid + "\" | findstr " + pid + "\"");
        return executeProcess(processBuilder);
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
