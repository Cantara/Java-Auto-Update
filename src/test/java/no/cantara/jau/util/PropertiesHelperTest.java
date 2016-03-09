package no.cantara.jau.util;

import java.util.Map;
import java.util.Properties;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
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

    @Test
    public void testPropertiesAsMap() {
        Properties properties = new Properties();
        properties.put("key1", "value1");
        properties.put("key2", "value2");
        properties.put("key3", 42);

        Map<String, String> map = PropertiesHelper.propertiesAsMap(properties);
        assertEquals(3, map.size());
        assertEquals("value1", map.get("key1"));
        assertEquals("value2", map.get("key2"));
        assertEquals("42", map.get("key3"));
    }
}
