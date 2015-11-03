package no.cantara.jau.coms;

import no.cantara.jau.ApplicationProcess;
import no.cantara.jau.JavaAutoUpdater;
import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.serviceconfig.dto.CheckForUpdateRequest;
import org.testng.annotations.Test;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.NoContentException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;

import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by jorunfa on 29/10/15.
 */
public class CheckForUpdateHelperTest {

    @Test
    public void testPreconditionFailed() throws IOException {
        ConfigServiceClient configServiceClient = mock(ConfigServiceClient.class);
        when(configServiceClient.getApplicationState()).thenReturn(new Properties());
        when(configServiceClient.checkForUpdate(anyString(), any(CheckForUpdateRequest.class))).thenThrow(IllegalStateException.class);

        ApplicationProcess processHolder = mock(ApplicationProcess.class);
        ScheduledFuture processMonitorHandle = mock(ScheduledFuture.class);
        JavaAutoUpdater jau = mock(JavaAutoUpdater.class);

        Runnable checkForUpdateRunnable = CheckForUpdateHelper.getCheckForUpdateRunnable(1, configServiceClient, processHolder, processMonitorHandle, jau);
        checkForUpdateRunnable.run();

        verify(jau).registerClient();
        verify(configServiceClient).cleanApplicationState();
    }

    @Test
    public void testNotChanged() throws IOException {
        ConfigServiceClient configServiceClient = mock(ConfigServiceClient.class);
        when(configServiceClient.getApplicationState()).thenReturn(new Properties());
        when(configServiceClient.checkForUpdate(anyString(), any(CheckForUpdateRequest.class))).thenThrow(NoContentException.class);

        ApplicationProcess processHolder = mock(ApplicationProcess.class);
        ScheduledFuture processMonitorHandle = mock(ScheduledFuture.class);
        JavaAutoUpdater jau = mock(JavaAutoUpdater.class);

        Runnable checkForUpdateRunnable = CheckForUpdateHelper.getCheckForUpdateRunnable(1, configServiceClient, processHolder, processMonitorHandle, jau);
        checkForUpdateRunnable.run();

        verify(processHolder, never()).stopProcess();
    }

    @Test
    public void testServerProblems() throws IOException {
        ConfigServiceClient configServiceClient = mock(ConfigServiceClient.class);
        when(configServiceClient.getApplicationState()).thenReturn(new Properties());
        when(configServiceClient.checkForUpdate(anyString(), any(CheckForUpdateRequest.class))).thenThrow(InternalServerErrorException.class);

        ApplicationProcess processHolder = mock(ApplicationProcess.class);
        ScheduledFuture processMonitorHandle = mock(ScheduledFuture.class);
        JavaAutoUpdater jau = mock(JavaAutoUpdater.class);

        Runnable checkForUpdateRunnable = CheckForUpdateHelper.getCheckForUpdateRunnable(1, configServiceClient, processHolder, processMonitorHandle, jau);
        checkForUpdateRunnable.run();

        verify(processHolder, never()).stopProcess();
    }

}
