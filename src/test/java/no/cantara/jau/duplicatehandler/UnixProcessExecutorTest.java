package no.cantara.jau.duplicatehandler;

import org.testng.annotations.Test;

import java.io.IOException;

public class UnixProcessExecutorTest {
    @Test
    public void shouldKillProcessByProcessName() throws IOException, InterruptedException {
        ProcessExecutor processExecutor = new UnixProcessExecutor();

        processExecutor.killProcessByProcessName("1234");
    }
}
