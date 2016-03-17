package no.cantara.jau.coms;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import no.cantara.cs.client.ConfigServiceClient;
import no.cantara.cs.dto.ClientConfig;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;

public class RegisterClientHelper {

    private final ConfigServiceClient configServiceClient;
    private String clientId;
    private final String artifactId;
    private final String clientName;

    public RegisterClientHelper(ConfigServiceClient configServiceClient, String artifactId, String clientName, String clientId) {
        this.artifactId = artifactId;
        this.clientName = clientName;
        this.configServiceClient = configServiceClient;
        this.clientId = clientId;
    }

    public ClientConfig registerClient() {
        BackOff exponentialBackOff = new ExponentialBackOff();
        BackOffExecution backOffExecution = exponentialBackOff.start();

        while (true) {
            try {
                return new CommandRegisterClient(artifactId, configServiceClient, clientName, clientId).execute();
            } catch (HystrixRuntimeException e) {
                RegisterClientExceptionHandler.handleRegisterClientException(e, exponentialBackOff, backOffExecution,
                        configServiceClient.getUrl());
            }
        }
    }

}
