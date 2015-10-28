package no.cantara.jau.coms;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import org.testng.annotations.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.NoContentException;
import java.io.IOException;

import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Created by jorunfa on 28/10/15.
 */
public class CommandRegisterClientTest {

    @Test
    public void test404ShouldThrowHystrixRuntimeException() throws IOException {
        ConfigServiceClient configServiceClient = mock(ConfigServiceClient.class);
        when(configServiceClient.checkForUpdate(anyString(), anyString(), anyMap())).thenThrow(
                new NotFoundException("test not found message"));

        try {
            new CommandRegisterClient("", configServiceClient, "").execute();
        } catch (HystrixRuntimeException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof NotFoundException);
        } catch (Exception e) {
            fail("Should not get another exception.");
        }
    }

    @Test
    public void testInternalServerErrorShouldThrowHystrixRuntimeException() throws IOException {
        ConfigServiceClient configServiceClient = mock(ConfigServiceClient.class);
        when(configServiceClient.checkForUpdate(anyString(), anyString(), anyMap())).thenThrow(
                new InternalServerErrorException("test InternalServerErrorException message"));

        try {
            new CommandRegisterClient("", configServiceClient, "").execute();
        } catch (HystrixRuntimeException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof InternalServerErrorException);
        } catch (Exception e) {
            fail("Should not get another exception.");
        }
    }

    @Test
    public void testBadRequestExceptionShouldThrowHystrixRuntimeException() throws IOException {
        ConfigServiceClient configServiceClient = mock(ConfigServiceClient.class);
        when(configServiceClient.checkForUpdate(anyString(), anyString(), anyMap())).thenThrow(
                new BadRequestException("test BadRequestException message"));

        try {
            new CommandRegisterClient("", configServiceClient, "").execute();
        } catch (HystrixRuntimeException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof BadRequestException);
        } catch (Exception e) {
            fail("Should not get another exception.");
        }
    }

    @Test
    public void testNoContentExceptionShouldThrowHystrixRuntimeException() throws IOException {
        ConfigServiceClient configServiceClient = mock(ConfigServiceClient.class);
        when(configServiceClient.checkForUpdate(anyString(), anyString(), anyMap())).thenThrow(
                new NoContentException("test NoContentException message"));

        try {
            new CommandRegisterClient("", configServiceClient, "").execute();
        } catch (HystrixRuntimeException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof NoContentException);
        } catch (Exception e) {
            fail("Should not get another exception.");
        }
    }


    @Test
    public void testIllegalStateExceptionShouldThrowHystrixRuntimeException() throws IOException {
        ConfigServiceClient configServiceClient = mock(ConfigServiceClient.class);
        when(configServiceClient.checkForUpdate(anyString(), anyString(), anyMap())).thenThrow(
                new IllegalStateException("test NoContentException message"));

        try {
            new CommandRegisterClient("", configServiceClient, "").execute();
        } catch (HystrixRuntimeException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof IllegalStateException);
        } catch (Exception e) {
            fail("Should not get another exception.");
        }
    }


}
