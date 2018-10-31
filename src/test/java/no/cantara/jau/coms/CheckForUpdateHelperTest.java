package no.cantara.jau.coms;

import no.cantara.cs.client.ConfigServiceClient;
import no.cantara.cs.client.HttpException;
import no.cantara.cs.dto.ApplicationConfig;
import no.cantara.cs.dto.CheckForUpdateRequest;
import no.cantara.cs.dto.ClientConfig;
import no.cantara.jau.ApplicationProcess;
import no.cantara.jau.JavaAutoUpdater;
import no.cantara.jau.eventextraction.EventExtractorService;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;

import static org.mockito.Mockito.*;

/**
 * Created by jorunfa on 29/10/15.
 */
public class CheckForUpdateHelperTest {

    @Test
    public void testPreconditionFailed() throws IOException {
        ConfigServiceClient configServiceClient = mock(ConfigServiceClient.class);
        when(configServiceClient.getApplicationState()).thenReturn(new Properties());
        when(configServiceClient.checkForUpdate(any(), any())).thenThrow(new HttpException(HttpURLConnection.HTTP_PRECON_FAILED, "precondition failed"));

        ApplicationProcess processHolder = mock(ApplicationProcess.class);
        ScheduledFuture processMonitorHandle = mock(ScheduledFuture.class);
        JavaAutoUpdater jau = mock(JavaAutoUpdater.class);

        ApplicationConfig newConfig = new ApplicationConfig("name");
        newConfig.setStartServiceScript("start script");
        ClientConfig newClientConfig = new ClientConfig("clientId", newConfig);
        when(jau.registerClient()).thenReturn(newClientConfig);

        EventExtractorService extractorService = mock(EventExtractorService.class);

        Runnable checkForUpdateRunnable = CheckForUpdateHelper.getCheckForUpdateRunnable(1, configServiceClient,
                processHolder, processMonitorHandle, extractorService, jau
        );
        checkForUpdateRunnable.run();

        verify(jau).registerClient();
        verify(configServiceClient).cleanApplicationState();
        verify(jau).storeClientFiles(newClientConfig);
        verify(configServiceClient).saveApplicationState(newClientConfig);
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
        when(configServiceClient.checkForUpdate(anyString(), any(CheckForUpdateRequest.class))).thenThrow(new HttpException(HttpURLConnection.HTTP_INTERNAL_ERROR, "internal error"));

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
    public void testUnknownHttpError() throws IOException {
        ConfigServiceClient configServiceClient = mock(ConfigServiceClient.class);
        when(configServiceClient.getApplicationState()).thenReturn(new Properties());
        when(configServiceClient.checkForUpdate(anyString(), any(CheckForUpdateRequest.class))).thenThrow(new HttpException(HttpURLConnection.HTTP_BAD_GATEWAY, "bad gateway"));

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
    public void testIOConnection() throws IOException {
        ConfigServiceClient configServiceClient = mock(ConfigServiceClient.class);
        when(configServiceClient.getApplicationState()).thenReturn(new Properties());
        when(configServiceClient.checkForUpdate(anyString(), any(CheckForUpdateRequest.class))).thenThrow(new IOException());

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
