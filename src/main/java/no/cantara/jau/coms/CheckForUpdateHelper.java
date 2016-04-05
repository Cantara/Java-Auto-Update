package no.cantara.jau.coms;

import no.cantara.cs.client.ConfigServiceClient;
import no.cantara.cs.client.EventExtractionUtil;
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

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.NoContentException;
import java.io.IOException;
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
            } catch (IllegalStateException e) {
                log.debug("Illegal state - reregister client");
                log.warn(e.getMessage());
                configServiceClient.cleanApplicationState();
                newClientConfig = jau.registerClient();
            } catch (NoContentException e) {
                log.error("Got NoContentException: ", e);
                return;
            } catch (BadRequestException e) {
                log.error("Got BadRequestException: ", e);
                return;
            } catch (InternalServerErrorException e) {
                log.warn("Got InternalServerErrorException: ", e);
                return;
            } catch (Exception e) {
                log.error("checkForUpdate failed, do nothing. Retrying in {} seconds.", interval, e);
                return;
            }

            if (newClientConfig == null) {
                log.debug("No updated config.");
                extractorService.clearRepo();
                return;
            }

            // ExecutorService swallows any exceptions silently, so need to handle them explicitly.
            // See http://www.nurkiewicz.com/2014/11/executorservice-10-tips-and-tricks.html (point 6.).
            try {
                log.debug("We got changes - stopping process and downloading new files.");
                processHolder.stopProcess();

                jau.storeClientFiles(newClientConfig);

                String[] command = newClientConfig.config.getStartServiceScript().split("\\s+");
                processHolder.setCommand(command);
                processHolder.setClientId(newClientConfig.clientId);
                processHolder.setLastChangedTimestamp(newClientConfig.config.getLastChanged());

                configServiceClient.saveApplicationState(newClientConfig);
            } catch (Exception e) {
                log.warn("Error thrown from scheduled lambda.", e);
            }
        };
    }

}
