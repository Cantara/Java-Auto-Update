package no.cantara.jau.util;

import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Created by jorunfa on 11/11/15.
 */
public class ClientEnvironmentUtilTest {

    @Test
    public void testGetClientEnvironment() {
        SortedMap<String, String> clientEnvironment = ClientEnvironmentUtil.getClientEnvironment();

        assertNotNull(clientEnvironment.get("jau.version"));
    }

    @Test
    public void testGetClientEnvironmentOK() {
        SortedMap<String, String> clientEnvironment = ClientEnvironmentUtil.getClientEnvironment();
        int nicCount = 0;
        for (Map.Entry<String, String> entry : clientEnvironment.entrySet()) {
            //System.out.println(entry);
            if (entry.getKey().startsWith(ClientEnvironmentUtil.NETWORKINTERFACE)) {
                nicCount++;
            }
        }
        assertTrue(nicCount > 0);
    }

    @Test
    public void testMaskEnvVariables() {
        Set<String> variablesToMask = new HashSet<>();
        variablesToMask.add("null-value");
        variablesToMask.add("empty-value");
        variablesToMask.add("length1");
        variablesToMask.add("length4");
        variablesToMask.add("length5");
        variablesToMask.add("length10");

        Map<String, String> environmentVariables = new HashMap<>();
        environmentVariables.put("null-value", null);
        environmentVariables.put("empty-value", "");
        environmentVariables.put("length1", "1");
        environmentVariables.put("length4", "1234");
        environmentVariables.put("length5", "12345");
        environmentVariables.put("length10", "1234567890");
        environmentVariables.put("unmasked-variable", "unmasked");

        Map<String, String> maskedProperties = ClientEnvironmentUtil.maskApplicationEnvProperties(environmentVariables, variablesToMask);
        assertEquals(maskedProperties.get("null-value"), null);
        assertEquals(maskedProperties.get("empty-value"), "");
        assertEquals(maskedProperties.get("length1"), "1");
        assertEquals(maskedProperties.get("length4"), "1234");
        assertEquals(maskedProperties.get("length5"), "12...45");
        assertEquals(maskedProperties.get("length10"), "12...90");
        assertEquals(maskedProperties.get("unmasked-variable"), "unmasked");
    }

}
