package no.cantara.jau.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * Created by jorunfa on 11/11/15.
 */
public class PropertiesHelperTest {

    @Test
    public void testGetVersion() {
        String version = PropertiesHelper.getVersion();

        assertTrue(version.contains("."));
    }

}
