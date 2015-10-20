package no.cantara.jau.coms;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import no.cantara.jau.util.SleepUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

public class RegisterClientExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RegisterClientHelper.class);

    public static void handleRegisterClientException(HystrixRuntimeException e, ExponentialBackOff exponentialBackOff,
                                                     BackOffExecution backOffExecution, String serviceConfigUrl) {
        Throwable cause = e.getCause();
        log.debug("Exception registering client, exception getMessage={}", e.getMessage());
        log.debug("Exception registering client, cause getMessage={}", cause.getMessage());

        if (cause instanceof ConnectException) {
            log.debug("Connection refused to ConfigService url={}", serviceConfigUrl);
        } else if (cause instanceof InternalServerErrorException) {
            log.debug("Internal server error in ConfigService url={}", serviceConfigUrl);
        } else if(cause instanceof NotFoundException) {
            log.debug("404 not found to ConfigService url={}", serviceConfigUrl);
        } else if (cause instanceof BadRequestException) {
            log.error("400 Bad Request. Probably need to fix something on the client. Exiting after a" +
                    " wait, so as to not DDoS the server.");
            SleepUtil.sleepWithLogging(exponentialBackOff.getMaxInterval() * 2);
            System.exit(1);
        } else if (cause instanceof TimeoutException) {
            log.debug("CommandRegisterClient timed out.");
        } else {
            log.error("Couldn't handle exception: {}", e);
        }

        SleepUtil.sleepWithLogging(backOffExecution.nextBackOff());
    }

}
