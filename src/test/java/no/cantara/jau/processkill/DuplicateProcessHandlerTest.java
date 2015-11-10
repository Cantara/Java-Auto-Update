package no.cantara.jau.processkill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class DuplicateProcessHandlerTest {
    private static final Logger log = LoggerFactory.getLogger(DuplicateProcessHandlerTest.class);

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
    public void shouldReturnTrueWhenProcessIsKilled() throws IOException, InterruptedException {
        ProcessExecutorFetcher processExecutorFetcher = mock(ProcessExecutorFetcher.class);
        ProcessExecutor processExecutorMock = mock(ProcessExecutor.class);
        when(processExecutorFetcher.getProcessExecutorBasedOnOs()).thenReturn(processExecutorMock);
        LastRunningProcessFileUtil lastRunningProcessFileUtilMock = mock(LastRunningProcessFileUtil.class);

        when(lastRunningProcessFileUtilMock.getRunningProcessPidFromFile()).thenReturn("12345");
        when(processExecutorMock.isProcessRunning("12345")).thenReturn(true);
        when(processExecutorMock.killProcessByPID("12345")).thenReturn(true);

        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(processExecutorFetcher,
                lastRunningProcessFileUtilMock);
        boolean result = duplicateProcessHandler.killExistingProcessIfRunning("processCommand");

        Assert.assertTrue(result);
    }

    @Test
    public void shouldReturnFalseWhenKillingByPidFailsAndFallbackToKillByProcessNameFails() throws IOException, InterruptedException {
        ProcessExecutorFetcher processExecutorFetcher = mock(ProcessExecutorFetcher.class);
        ProcessExecutor processExecutorMock = mock(ProcessExecutor.class);
        when(processExecutorFetcher.getProcessExecutorBasedOnOs()).thenReturn(processExecutorMock);
        LastRunningProcessFileUtil lastRunningProcessFileUtilMock = mock(LastRunningProcessFileUtil.class);

        when(lastRunningProcessFileUtilMock.getRunningProcessPidFromFile()).thenReturn("12345");
        when(processExecutorMock.isProcessRunning("12345")).thenReturn(true);
        when(processExecutorMock.killProcessByPID("12345")).thenReturn(false);
        when(processExecutorMock.killProcessByProcessName(anyString())).thenReturn(false);

        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(processExecutorFetcher,
                lastRunningProcessFileUtilMock);
        boolean result = duplicateProcessHandler.killExistingProcessIfRunning("java -jar java_application-123.alpha-2.jar");

        Assert.assertFalse(result);
    }

    @Test
    public void shouldFallbackToKillingByProcessNameWhenErrorReadingPidFile() throws IOException, InterruptedException {
        ProcessExecutorFetcher processExecutorFetcher = mock(ProcessExecutorFetcher.class);
        ProcessExecutor processExecutorMock = mock(ProcessExecutor.class);
        when(processExecutorFetcher.getProcessExecutorBasedOnOs()).thenReturn(processExecutorMock);
        LastRunningProcessFileUtil lastRunningProcessFileUtilMock = mock(LastRunningProcessFileUtil.class);

        doThrow(new IOException()).when(lastRunningProcessFileUtilMock).getRunningProcessPidFromFile();
        when(processExecutorMock.killProcessByProcessName("java_application-123.alpha-2.jar")).thenReturn(true);

        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(processExecutorFetcher,
                lastRunningProcessFileUtilMock);
        boolean result = duplicateProcessHandler.killExistingProcessIfRunning("java -jar java_application-123.alpha-2.jar");

        verify(processExecutorMock, times(1)).killProcessByProcessName("java_application-123.alpha-2.jar");
        Assert.assertTrue(result);
    }

    @Test
    public void shouldReturnTrueWhenProcessIsNotRunning() throws IOException, InterruptedException {
        ProcessExecutorFetcher processExecutorFetcher = mock(ProcessExecutorFetcher.class);
        ProcessExecutor processExecutorMock = mock(ProcessExecutor.class);
        when(processExecutorFetcher.getProcessExecutorBasedOnOs()).thenReturn(processExecutorMock);
        LastRunningProcessFileUtil lastRunningProcessFileUtilMock = mock(LastRunningProcessFileUtil.class);

        when(lastRunningProcessFileUtilMock.getRunningProcessPidFromFile()).thenReturn("12345");
        when(processExecutorMock.isProcessRunning("12345")).thenReturn(false);

        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(processExecutorFetcher,
                lastRunningProcessFileUtilMock);
        boolean result = duplicateProcessHandler.killExistingProcessIfRunning("processCommand");

        Assert.assertTrue(result);
    }

    @Test
    public void shouldReturnTrueWhenPIDFileDoesNotExist() throws IOException {
        ProcessExecutorFetcher processExecutorFetcher = mock(ProcessExecutorFetcher.class);
        ProcessExecutor processExecutorMock = mock(ProcessExecutor.class);
        when(processExecutorFetcher.getProcessExecutorBasedOnOs()).thenReturn(processExecutorMock);
        LastRunningProcessFileUtil lastRunningProcessFileUtilMock = mock(LastRunningProcessFileUtil.class);

        when(lastRunningProcessFileUtilMock.getRunningProcessPidFromFile()).thenReturn(null);

        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(processExecutorFetcher,
                lastRunningProcessFileUtilMock);
        boolean result = duplicateProcessHandler.killExistingProcessIfRunning("processCommand");

        Assert.assertTrue(result);
    }

    @Test
    public void shouldReturnFalseWhenReadingOfPIDFileThrowsException() throws IOException {
        ProcessExecutorFetcher processExecutorFetcher = mock(ProcessExecutorFetcher.class);
        ProcessExecutor processExecutorMock = mock(ProcessExecutor.class);
        when(processExecutorFetcher.getProcessExecutorBasedOnOs()).thenReturn(processExecutorMock);
        LastRunningProcessFileUtil lastRunningProcessFileUtilMock = mock(LastRunningProcessFileUtil.class);

        when(lastRunningProcessFileUtilMock.getRunningProcessPidFromFile()).thenThrow(new IOException());

        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(processExecutorFetcher,
                lastRunningProcessFileUtilMock);
        boolean result = duplicateProcessHandler.killExistingProcessIfRunning("processCommand");

        Assert.assertFalse(result);
    }

    @Test
    public void shouldReturnFalseWhenPIDIsInvalid() throws IOException {
        ProcessExecutorFetcher processExecutorFetcher = mock(ProcessExecutorFetcher.class);
        ProcessExecutor processExecutorMock = mock(ProcessExecutor.class);
        when(processExecutorFetcher.getProcessExecutorBasedOnOs()).thenReturn(processExecutorMock);
        LastRunningProcessFileUtil lastRunningProcessFileUtilMock = mock(LastRunningProcessFileUtil.class);

        when(lastRunningProcessFileUtilMock.getRunningProcessPidFromFile()).thenReturn("blablainvalid");

        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(processExecutorFetcher,
                lastRunningProcessFileUtilMock);
        boolean result = duplicateProcessHandler.killExistingProcessIfRunning("processCommand");

        Assert.assertFalse(result);
    }

    @Test
    public void shouldReturnFalseWhenCannotDetectIfProcessIsRunning() throws IOException, InterruptedException {
        ProcessExecutorFetcher processExecutorFetcher = mock(ProcessExecutorFetcher.class);
        ProcessExecutor processExecutorMock = mock(ProcessExecutor.class);
        when(processExecutorFetcher.getProcessExecutorBasedOnOs()).thenReturn(processExecutorMock);
        LastRunningProcessFileUtil lastRunningProcessFileUtilMock = mock(LastRunningProcessFileUtil.class);

        when(lastRunningProcessFileUtilMock.getRunningProcessPidFromFile()).thenReturn("13245");
        when(processExecutorMock.isProcessRunning("13245")).thenThrow(new IOException());

        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(processExecutorFetcher,
                lastRunningProcessFileUtilMock);
        boolean result = duplicateProcessHandler.killExistingProcessIfRunning("processCommand");

        Assert.assertFalse(result);
    }

    @Test
    public void shouldReturnFalseWhenProcessCannotBeKilledByPidOrName() throws IOException, InterruptedException {
        ProcessExecutorFetcher processExecutorFetcher = mock(ProcessExecutorFetcher.class);
        ProcessExecutor processExecutorMock = mock(ProcessExecutor.class);
        when(processExecutorFetcher.getProcessExecutorBasedOnOs()).thenReturn(processExecutorMock);
        LastRunningProcessFileUtil lastRunningProcessFileUtilMock = mock(LastRunningProcessFileUtil.class);

        when(lastRunningProcessFileUtilMock.getRunningProcessPidFromFile()).thenReturn("13245");
        when(processExecutorMock.isProcessRunning("13245")).thenReturn(true);
        doThrow(new IOException()).when(processExecutorMock).killProcessByPID("13245");

        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(processExecutorFetcher,
                lastRunningProcessFileUtilMock);
        boolean result = duplicateProcessHandler.killExistingProcessIfRunning("processCommand");

        Assert.assertFalse(result);
    }

    @Test
    public void shouldExtractProcessNameFromCommand() throws IOException, InterruptedException {
        ProcessExecutorFetcher processExecutorFetcher = mock(ProcessExecutorFetcher.class);
        ProcessExecutor processExecutorMock = mock(ProcessExecutor.class);
        when(processExecutorFetcher.getProcessExecutorBasedOnOs()).thenReturn(processExecutorMock);
        LastRunningProcessFileUtil lastRunningProcessFileUtilMock = mock(LastRunningProcessFileUtil.class);

        when(lastRunningProcessFileUtilMock.getRunningProcessPidFromFile()).thenReturn("13245");
        when(processExecutorMock.isProcessRunning("13245")).thenReturn(true);
        doThrow(new IOException()).when(processExecutorMock).killProcessByPID("13245");

        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(processExecutorFetcher,
                lastRunningProcessFileUtilMock);

        String processName = duplicateProcessHandler.extractProcessNameFromCommand("java -jar pharmacy-agent-0.8-SNAPSHOT.jar");

        Assert.assertEquals(processName, "pharmacy-agent-0.8-SNAPSHOT.jar");
    }

}
