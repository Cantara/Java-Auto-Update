package no.cantara.jau.util;

import org.testng.annotations.Test;

import java.util.SortedMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Created by jorunfa on 11/11/15.
 */
public class ClientEnvironmentUtilTest {

    @Test
    public void testGetClientEnvironment() {
        SortedMap<String, String> clientEnvironment = ClientEnvironmentUtil.getClientEnvironment();

        assertNotNull(clientEnvironment.get("jau.version"));
    }

}
