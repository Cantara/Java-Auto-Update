package no.cantara.jau.coms;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import no.cantara.cs.client.ConfigServiceClient;
import no.cantara.cs.dto.ClientConfig;
import no.cantara.cs.dto.ClientRegistrationRequest;
import no.cantara.jau.util.ClientEnvironmentUtil;

import java.io.IOException;

public class CommandRegisterClient extends HystrixCommand<ClientConfig> {

    private static final int COMMAND_TIMEOUT = 5000;
    private static final String GROUP_KEY = "GROUP_KEY";
    private final String artifactId;
    private ConfigServiceClient configServiceClient;
    private final String clientName;
    private String clientId;

    public CommandRegisterClient(String artifactId, ConfigServiceClient configServiceClient, String clientName, String clientId) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(GROUP_KEY))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionTimeoutInMilliseconds(COMMAND_TIMEOUT)));
        this.artifactId = artifactId;
        this.configServiceClient = configServiceClient;
        this.clientName = clientName;
        this.clientId = clientId;
    }

    @Override
    protected ClientConfig run() throws IOException {
        ClientRegistrationRequest registrationRequest = new ClientRegistrationRequest(artifactId);
        registrationRequest.envInfo.putAll(ClientEnvironmentUtil.getClientEnvironment());
        registrationRequest.clientName = clientName;
        registrationRequest.clientId = clientId;

        ClientConfig clientConfig = configServiceClient.registerClient(registrationRequest);

        configServiceClient.saveApplicationState(clientConfig);
        return clientConfig;
    }

}
