package no.cantara.jau.processkill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class DuplicateProcessHandlerTest {
    private static final Logger log = LoggerFactory.getLogger(DuplicateProcessHandlerIntTest.class);

    @Test
    public void shouldFindProcessIdAndWriteToFile() throws ReflectiveOperationException {
        ProcessExecutorFetcher processExecutorFetcher = mock(ProcessExecutorFetcher.class);
        ProcessExecutor processExecutorMock = mock(ProcessExecutor.class);
        when(processExecutorFetcher.getProcessExecutorBasedOnOs()).thenReturn(processExecutorMock);
        LastRunningProcessFileUtil lastRunningProcessFileUtilMock = mock(LastRunningProcessFileUtil.class);
        stub(processExecutorMock.findProcessId(any())).toReturn("12345");

        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(processExecutorFetcher,
                lastRunningProcessFileUtilMock);
        duplicateProcessHandler.findRunningManagedProcessPidAndWriteToFile(any(Process.class));

        verify(lastRunningProcessFileUtilMock, times(1)).writePidToFile("12345");
    }

    @Test
    public void shouldKillProcess() throws IOException, InterruptedException {
        ProcessExecutorFetcher processExecutorFetcher = mock(ProcessExecutorFetcher.class);
        ProcessExecutor processExecutorMock = mock(ProcessExecutor.class);
        when(processExecutorFetcher.getProcessExecutorBasedOnOs()).thenReturn(processExecutorMock);
        LastRunningProcessFileUtil lastRunningProcessFileUtilMock = mock(LastRunningProcessFileUtil.class);
        when(lastRunningProcessFileUtilMock.getRunningProcessPidFromFile()).thenReturn("12345");
        when(processExecutorMock.isProcessRunning("12345")).thenReturn(true);

        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(processExecutorFetcher,
                lastRunningProcessFileUtilMock);
        boolean result = duplicateProcessHandler.killExistingProcessIfRunning();

        Assert.assertTrue(result);
    }

    @Test
    public void shouldReturnTrueToKillProcessWhenProcessIsNotRunning() throws IOException, InterruptedException {
        ProcessExecutorFetcher processExecutorFetcher = mock(ProcessExecutorFetcher.class);
        ProcessExecutor processExecutorMock = mock(ProcessExecutor.class);
        when(processExecutorFetcher.getProcessExecutorBasedOnOs()).thenReturn(processExecutorMock);
        LastRunningProcessFileUtil lastRunningProcessFileUtilMock = mock(LastRunningProcessFileUtil.class);
        when(lastRunningProcessFileUtilMock.getRunningProcessPidFromFile()).thenReturn("12345");
        when(processExecutorMock.isProcessRunning("12345")).thenReturn(false);

        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(processExecutorFetcher,
                lastRunningProcessFileUtilMock);
        boolean result = duplicateProcessHandler.killExistingProcessIfRunning();

        Assert.assertFalse(result);
    }

}
