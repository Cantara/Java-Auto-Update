package no.cantara.jau;

import org.testng.annotations.Test;

import java.util.Map;
import java.util.SortedMap;

import static org.testng.Assert.assertTrue;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-09-20.
 */
public class ClientEnvironmentUtilTest {

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
}
