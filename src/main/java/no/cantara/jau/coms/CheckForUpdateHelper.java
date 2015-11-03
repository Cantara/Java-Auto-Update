package no.cantara.jau.coms;

import no.cantara.jau.ApplicationProcess;
import no.cantara.jau.JavaAutoUpdater;
import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.serviceconfig.dto.CheckForUpdateRequest;
import no.cantara.jau.serviceconfig.dto.ClientConfig;
import no.cantara.jau.util.ClientEnvironmentUtil;
import no.cantara.jau.util.PropertiesHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.NoContentException;
import java.io.IOException;
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
                                                     JavaAutoUpdater jau) {
        return () -> {
            ClientConfig newClientConfig = null;
            try {
                log.debug("Checking for updates. Inside lambda.");
                Properties applicationState = configServiceClient.getApplicationState();
                String clientId = PropertiesHelper.getStringProperty(applicationState, ConfigServiceClient.CLIENT_ID, null);
                String lastChanged = PropertiesHelper.getStringProperty(applicationState, ConfigServiceClient.LAST_CHANGED, null);
                SortedMap<String, String> clientEnvironment = ClientEnvironmentUtil.getClientEnvironment();
                String clientName = PropertiesHelper.getClientNameFromProperties(applicationState);
                CheckForUpdateRequest checkForUpdateRequest = new CheckForUpdateRequest(lastChanged, clientEnvironment, clientName);
                newClientConfig = configServiceClient.checkForUpdate(clientId, checkForUpdateRequest);
            } catch (IllegalStateException e) {
                log.debug("Illegal state - reregister client");
                log.warn(e.getMessage());
                configServiceClient.cleanApplicationState();
                newClientConfig = jau.registerClient();
                return;
            } catch (NoContentException e) {
                log.debug("No updated config.");
                return;
            } catch (BadRequestException e) {
                log.error("Got BadRequestException: ", e);
                return;
            } catch (InternalServerErrorException e) {
                log.warn("Got InternalServerErrorException: ", e);
                return;
            } catch (IOException e) {
                log.error("checkForUpdate failed, do nothing. Retrying in {} seconds.", interval, e);
                return;
            }

            // ExecutorService swallows any exceptions silently, so need to handle them explicitly.
            // See http://www.nurkiewicz.com/2014/11/executorservice-10-tips-and-tricks.html (point 6.).
            try {
                log.debug("We got changes - stopping process and downloading new files.");
                processHolder.stopProcess();

                jau.storeClientFiles(newClientConfig);

                String[] command = newClientConfig.serviceConfig.getStartServiceScript().split("\\s+");
                processHolder.setCommand(command);
                processHolder.setClientId(newClientConfig.clientId);
                processHolder.setLastChangedTimestamp(newClientConfig.serviceConfig.getLastChanged());

                configServiceClient.saveApplicationState(newClientConfig);

                processMonitorHandle.notify();
            } catch (Exception e) {
                log.warn("Error thrown from scheduled lambda.", e);
            }
        };
    }

}
