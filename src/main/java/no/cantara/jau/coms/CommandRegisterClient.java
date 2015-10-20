package no.cantara.jau.coms;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import no.cantara.jau.util.ClientEnvironmentUtil;
import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.serviceconfig.dto.ClientConfig;
import no.cantara.jau.serviceconfig.dto.ClientRegistrationRequest;

import javax.ws.rs.NotFoundException;

public class CommandRegisterClient extends HystrixCommand<ClientConfig> {

    private static final int COMMAND_TIMEOUT = 5000;
    private static final String GROUP_KEY = "GROUP_KEY";
    private ClientRegistrationRequest registrationRequest;
    private ConfigServiceClient configServiceClient;
    private final String clientName;

    public CommandRegisterClient(String artifactId, ConfigServiceClient configServiceClient, String clientName) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(GROUP_KEY))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionIsolationThreadTimeoutInMilliseconds(COMMAND_TIMEOUT)));
        this.registrationRequest = new ClientRegistrationRequest(artifactId);
        this.configServiceClient = configServiceClient;
        this.clientName = clientName;
    }

    @Override
    protected ClientConfig run() throws Exception {
        this.registrationRequest.envInfo.putAll(ClientEnvironmentUtil.getClientEnvironment());
        this.registrationRequest.clientName = clientName;
        ClientConfig clientConfig = configServiceClient.registerClient(registrationRequest);
        if (clientConfig == null) {
            throw new NotFoundException("got null clientConfig, indicating a 404 was the problem"); // I'm not so sure about this.
        }
        configServiceClient.saveApplicationState(clientConfig);
        return clientConfig;
    }

}
