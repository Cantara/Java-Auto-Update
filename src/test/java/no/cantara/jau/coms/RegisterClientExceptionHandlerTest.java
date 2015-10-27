package no.cantara.jau.coms;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.springframework.util.backoff.BackOffExecution;
import static org.mockito.Mockito.*;
import org.testng.annotations.Test;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

public class RegisterClientExceptionHandlerTest {

    @Test
    public void testShouldHandleConnectExceptionWithRetry() {
        BackOffExecution backOffExecution = mock(BackOffExecution.class);
        when(backOffExecution.nextBackOff()).thenReturn((long) 1);

        HystrixRuntimeException hystrixRuntimeException = mock(HystrixRuntimeException.class);
        when(hystrixRuntimeException.getCause()).thenReturn(new ConnectException());

        RegisterClientExceptionHandler.handleRegisterClientException(hystrixRuntimeException, null, backOffExecution, "");

        verify(backOffExecution).nextBackOff();
    }

    @Test
    public void testShouldHandleInternalServerErrorExceptionWithRetry() {
        BackOffExecution backOffExecution = mock(BackOffExecution.class);
        when(backOffExecution.nextBackOff()).thenReturn((long) 1);

        HystrixRuntimeException hystrixRuntimeException = mock(HystrixRuntimeException.class);
        when(hystrixRuntimeException.getCause()).thenReturn(new InternalServerErrorException());

        RegisterClientExceptionHandler.handleRegisterClientException(hystrixRuntimeException, null, backOffExecution, "");

        verify(backOffExecution).nextBackOff();
    }

    @Test
     public void testShouldHandleNotFoundExceptionWithRetry() {
        BackOffExecution backOffExecution = mock(BackOffExecution.class);
        when(backOffExecution.nextBackOff()).thenReturn((long) 1);

        HystrixRuntimeException hystrixRuntimeException = mock(HystrixRuntimeException.class);
        when(hystrixRuntimeException.getCause()).thenReturn(new NotFoundException());

        RegisterClientExceptionHandler.handleRegisterClientException(hystrixRuntimeException, null, backOffExecution, "");

        verify(backOffExecution).nextBackOff();
    }

    @Test
    public void testShouldHandleTimeoutExceptionWithRetry() {
        BackOffExecution backOffExecution = mock(BackOffExecution.class);
        when(backOffExecution.nextBackOff()).thenReturn((long) 1);

        HystrixRuntimeException hystrixRuntimeException = mock(HystrixRuntimeException.class);
        when(hystrixRuntimeException.getCause()).thenReturn(new TimeoutException());

        RegisterClientExceptionHandler.handleRegisterClientException(hystrixRuntimeException, null, backOffExecution, "");

        verify(backOffExecution).nextBackOff();
    }



}
