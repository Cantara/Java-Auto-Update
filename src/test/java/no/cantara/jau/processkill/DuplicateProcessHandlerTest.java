package no.cantara.jau.processkill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class DuplicateProcessHandlerTest {
    private static final Logger log = LoggerFactory.getLogger(DuplicateProcessHandlerIntTest.class);

    @Test
    public void shouldFindProcessIdAndWriteToFile() {
        ProcessAdapter processAdapterMock = mock(ProcessAdapter.class);
        LastRunningProcessFileUtil lastRunningProcessFileUtilMock = mock(LastRunningProcessFileUtil.class);
        stub(processAdapterMock.findProcessId(any())).toReturn("12345");

        DuplicateProcessHandler duplicateProcessHandler = new DuplicateProcessHandler(processAdapterMock,
                lastRunningProcessFileUtilMock);
        duplicateProcessHandler.findRunningManagedProcessPidAndWriteToFile(any(Process.class));

        verify(lastRunningProcessFileUtilMock, times(1)).writePidToFile("12345");
    }

}
