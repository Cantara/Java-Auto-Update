package no.cantara.jau.coms;

import no.cantara.cs.client.ConfigServiceClient;
import no.cantara.cs.dto.CheckForUpdateRequest;
import no.cantara.jau.ApplicationProcess;
import no.cantara.jau.JavaAutoUpdater;
import no.cantara.jau.eventextraction.EventExtractorService;
import org.testng.annotations.Test;

import javax.ws.rs.InternalServerErrorException;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;

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
        EventExtractorService extractorService = mock(EventExtractorService.class);

        Runnable checkForUpdateRunnable = CheckForUpdateHelper.getCheckForUpdateRunnable(1, configServiceClient,
                processHolder, processMonitorHandle, extractorService, jau
                );
        checkForUpdateRunnable.run();

        verify(jau).registerClient();
        verify(configServiceClient).cleanApplicationState();
    }

    @Test
    public void testNotChanged() throws IOException {
        ConfigServiceClient configServiceClient = mock(ConfigServiceClient.class);
        when(configServiceClient.getApplicationState()).thenReturn(new Properties());
        when(configServiceClient.checkForUpdate(anyString(), any(CheckForUpdateRequest.class))).thenReturn(null);

        ApplicationProcess processHolder = mock(ApplicationProcess.class);
        ScheduledFuture processMonitorHandle = mock(ScheduledFuture.class);
        JavaAutoUpdater jau = mock(JavaAutoUpdater.class);
        EventExtractorService extractorService = mock(EventExtractorService.class);

        Runnable checkForUpdateRunnable = CheckForUpdateHelper.getCheckForUpdateRunnable(1, configServiceClient,
                processHolder, processMonitorHandle, extractorService, jau);
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
        EventExtractorService extractorService = mock(EventExtractorService.class);

        Runnable checkForUpdateRunnable = CheckForUpdateHelper.getCheckForUpdateRunnable(1, configServiceClient,
                processHolder, processMonitorHandle, extractorService, jau);
        checkForUpdateRunnable.run();

        verify(processHolder, never()).stopProcess();
    }

}
