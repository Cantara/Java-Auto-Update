package no.cantara.jau.util;

import org.testng.annotations.Test;

import java.util.Properties;

import static org.testng.Assert.assertEquals;

/**
 * Created by jorunfa on 03/11/15.
 */
public class PropertiesHelperTest {

    @Test
    public void testClientNameSet() {
        Properties properties = new Properties();
        properties.setProperty("clientName", "THIS IS THE CLIENT NAME");

        String actual = PropertiesHelper.getClientName(properties);

        assertEquals(actual, "THIS IS THE CLIENT NAME");
    }

    @Test
    public void testClientNameNotSet() {
        Properties properties = new Properties();

        String actual = PropertiesHelper.getClientName(properties);

        assertEquals(actual, PropertiesHelper.CLIENT_NAME_PROPERTY_DEFAULT_VALUE);
    }

}
