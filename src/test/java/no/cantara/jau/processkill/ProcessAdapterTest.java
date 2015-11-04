package no.cantara.jau.processkill;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ProcessAdapterTest {

    @Test
    public void shouldKillRunningProcess() throws IOException, InterruptedException {
        ProcessExecutor processExecutorMock = mock(UnixProcessExecutor.class);
        ProcessExecutorFetcher processExecutorFetcher = mock(ProcessExecutorFetcher.class);
        when(processExecutorFetcher.getProcessExecutorBasedOnOs()).thenReturn(processExecutorMock);
        ProcessAdapter processAdapter = new ProcessAdapter(processExecutorFetcher);

        processAdapter.killRunningProcess("12345");

        verify(processExecutorMock, times(1)).killProcess("12345");
    }

    @Test
    public void shouldHandleExceptionWhenKillingRunningProcessFails() {
        ProcessExecutor processExecutorMock = mock(UnixProcessExecutor.class);
        ProcessExecutorFetcher processExecutorFetcher = mock(ProcessExecutorFetcher.class);
        when(processExecutorFetcher.getProcessExecutorBasedOnOs()).thenReturn(processExecutorMock);
        ProcessAdapter processAdapter = new ProcessAdapter(processExecutorFetcher);

        when(processAdapter.killRunningProcess("12345")).thenThrow(new IOException());

        boolean result = processAdapter.killRunningProcess("12345");

        Assert.assertFalse(result);
    }

    @Test
    public void shouldHandleExceptionWhenFindingProcessIdFails() {
        ProcessExecutor processExecutorMock = mock(UnixProcessExecutor.class);
        ProcessExecutorFetcher processExecutorFetcher = mock(ProcessExecutorFetcher.class);
        when(processExecutorFetcher.getProcessExecutorBasedOnOs()).thenReturn(processExecutorMock);
        ProcessAdapter processAdapter = new ProcessAdapter(processExecutorFetcher);

        when(processAdapter.findProcessId(any(Process.class))).thenThrow(new NoSuchFieldException());

        String pid = processAdapter.findProcessId(any());
        Assert.assertNull(pid);
    }

    @Test
    public void shouldHandleExceptionWhenProcessIsRunningFails() {
        ProcessExecutor processExecutorMock = mock(UnixProcessExecutor.class);
        ProcessExecutorFetcher processExecutorFetcher = mock(ProcessExecutorFetcher.class);
        when(processExecutorFetcher.getProcessExecutorBasedOnOs()).thenReturn(processExecutorMock);
        ProcessAdapter processAdapter = new ProcessAdapter(processExecutorFetcher);

        when(processAdapter.processIsRunning("12345")).thenThrow(new IOException());
        boolean result = processAdapter.processIsRunning("12345");

        Assert.assertFalse(result);
    }

//    @Test
//    public void shouldKillExistingProcessWhenExistingProcessIsRunning() throws ReflectiveOperationException,
//            InterruptedException, IOException {
//        ProcessExecutor processExecutorMock = mock(UnixProcessExecutor.class);
//        ProcessExecutorFetcher processExecutorFetcher = new ProcessExecutorFetcher();
//        stub(processExecutorFetcher.getProcessExecutorBasedOnOs()).toReturn(processExecutorMock);
//        ProcessAdapter processAdapter = new ProcessAdapter(processExecutorFetcher);
//        ProcessBuilder processBuilderMock = mock(ProcessBuilder.class);
//
//        stub(processExecutorFetcher.getProcessExecutorBasedOnOs()).toReturn(processExecutorMock);
//        stub(processExecutorMock.createKillProcessCommand("12345")).toReturn(processBuilderMock);
//        stub(processBuilderMock.start()).toReturn(mock(Process.class));
//    }
}
