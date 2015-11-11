package no.cantara.jau.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Created by jorunfa on 11/11/15.
 */
public class PropertiesHelperTest {

    @Test
    public void testGetVersion() {
        String version = PropertiesHelper.getVersion();

        assertEquals(version, "0.4-SNAPSHOT"); // This test has to be updated manually, due to the problem it solves.
    }

}
