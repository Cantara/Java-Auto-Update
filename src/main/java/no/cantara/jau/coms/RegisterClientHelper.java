package no.cantara.jau.coms;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.serviceconfig.dto.ClientConfig;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;

public class RegisterClientHelper {

    private final ConfigServiceClient configServiceClient;
    private final String serviceConfigUrl;
    private final String artifactId;
    private final String clientName;

    public RegisterClientHelper(ConfigServiceClient configServiceClient, String artifactId, String clientName,
                                String serviceConfigUrl) {
        this.artifactId = artifactId;
        this.clientName = clientName;
        this.serviceConfigUrl = serviceConfigUrl;
        this.configServiceClient = configServiceClient;
    }

    public ClientConfig registerClient() {
        ExponentialBackOff exponentialBackOff = new ExponentialBackOff();
        BackOffExecution backOffExecution = exponentialBackOff.start();

        while (true) {
            try {
                return new CommandRegisterClient(artifactId, configServiceClient, clientName).execute();
            } catch (HystrixRuntimeException e) {
                RegisterClientExceptionHandler.handleRegisterClientException(e, exponentialBackOff, backOffExecution, serviceConfigUrl);
            }
        }
    }

}
