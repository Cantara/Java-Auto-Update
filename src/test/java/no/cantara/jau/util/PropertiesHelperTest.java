package no.cantara.jau.util;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

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
    @Test
    public void testPropertiesPrecedence(){
        String filename = "target" + File.separator + "classes" + File.separator + " unit-test.properties";
        BufferedWriter bw = null;
        FileWriter fw = null;
        try {
            String content = "configservice.url=https://test.com"+System.getProperty("line.separator")+"configservice.username=ausername";
            fw = new FileWriter(filename);
            bw = new BufferedWriter(fw);
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null)
                    bw.close();
                if (fw != null)
                    fw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        String filename2 = System.getProperty("user.dir")+ File.separator + "config_override" + File.separator + "unit-test_overrides.properties";
        BufferedWriter bw2 = null;
        FileWriter fw2 = null;
        try {
            String content = "configservice.url=https://overridetest.com"+System.getProperty("line.separator");
            fw2 = new FileWriter(filename2);
            bw2 = new BufferedWriter(fw2);
            bw2.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw2 != null)
                    bw2.close();
                if (fw2 != null)
                    fw2.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        AppConfig.init("unit-test");
        PropertiesHelper.getPropertiesFromConfigFile(PropertiesHelper.JAU_CONFIG_FILENAME);
        assertEquals(PropertiesHelper.getConfigServiceUrl(),"https://overridetest.com");
        assertEquals(PropertiesHelper.getUsername(),"ausername");
    }
}
