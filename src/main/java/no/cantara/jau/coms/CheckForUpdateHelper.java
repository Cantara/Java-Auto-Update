package no.cantara.jau.coms;

import no.cantara.cs.client.ConfigServiceClient;
import no.cantara.cs.client.EventExtractionUtil;
import no.cantara.cs.client.HttpException;
import no.cantara.cs.dto.CheckForUpdateRequest;
import no.cantara.cs.dto.ClientConfig;
import no.cantara.cs.dto.event.Event;
import no.cantara.cs.dto.event.ExtractedEventsStore;
import no.cantara.jau.ApplicationProcess;
import no.cantara.jau.JavaAutoUpdater;
import no.cantara.jau.eventextraction.EventExtractorService;
import no.cantara.jau.util.ClientEnvironmentUtil;
import no.cantara.jau.util.PropertiesHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Created by jorunfa on 29/10/15.
 */
public class CheckForUpdateHelper {

    private static final Logger log = LoggerFactory.getLogger(CheckForUpdateHelper.class);

    public static Runnable getCheckForUpdateRunnable(long interval, ConfigServiceClient configServiceClient,
                                                     ApplicationProcess processHolder,
                                                     ScheduledFuture<?> processMonitorHandle,
                                                     EventExtractorService extractorService,
                                                     JavaAutoUpdater jau
                                                     ) {
        return () -> {
            ClientConfig newClientConfig = null;
            try {
                log.debug("Checking for updates. Inside lambda.");
                Properties applicationState = configServiceClient.getApplicationState();
                String clientId = PropertiesHelper.getStringProperty(applicationState, ConfigServiceClient.CLIENT_ID, null);
                String lastChanged = PropertiesHelper.getStringProperty(applicationState, ConfigServiceClient.LAST_CHANGED, null);
                SortedMap<String, String> clientEnvironment = ClientEnvironmentUtil.getClientEnvironment(applicationState,
                        String.valueOf(processHolder.processIsRunning()));

                List<Event> events = extractorService.extractEvents();
                ExtractedEventsStore eventsStore = EventExtractionUtil.mapToExtractedEvents(events);

                CheckForUpdateRequest checkForUpdateRequest = new CheckForUpdateRequest(lastChanged, clientEnvironment,
                        PropertiesHelper.getClientName(), eventsStore);
                newClientConfig = configServiceClient.checkForUpdate(clientId, checkForUpdateRequest);
            } catch (HttpException e) {
                if (e.getStatusCode() == HttpURLConnection.HTTP_PRECON_FAILED) {
                    log.warn("Got http {} Precondition failed, reregistering client. Response message: {}", e.getStatusCode(), e.getMessage());
                    configServiceClient.cleanApplicationState();
                    newClientConfig = jau.registerClient();
                } else if (e.getStatusCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
                    log.error("Got http {} Bad request: ", e.getStatusCode(), e);
                    return;
                } else if (e.getStatusCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                    log.warn("Got http {} Internal error: ", e.getStatusCode(), e);
                    return;
                } else {
                    log.warn("Got http {}. checkForUpdate failed, do nothing. Retrying in {} seconds.", e.getStatusCode(), interval, e);
                    return;
                }
            } catch (Throwable e) {
                log.warn("checkForUpdate failed, do nothing. Retrying in {} seconds.", interval, e);
                return;
            }

            // ExecutorService swallows any exceptions silently, so need to handle them explicitly.
            // See http://www.nurkiewicz.com/2014/11/executorservice-10-tips-and-tricks.html (point 6.).
            try {

                if (newClientConfig == null) {
                    log.debug("No updated config.");
                    extractorService.clearRepo();
                    return;
                }

                log.debug("We got changes - stopping process and downloading new files.");
                processHolder.stopProcess();

                jau.storeClientFiles(newClientConfig);

                String[] command = newClientConfig.config.getStartServiceScript().split("\\s+");
                processHolder.setCommand(command);
                processHolder.setClientId(newClientConfig.clientId);
                processHolder.setLastChangedTimestamp(newClientConfig.config.getLastChanged());

                configServiceClient.saveApplicationState(newClientConfig);
            } catch (Throwable e) {
                log.warn("Error thrown from scheduled lambda.", e);
            }
        };
    }

}
