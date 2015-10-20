package no.cantara.jau.util;

import java.util.Properties;

public class PropertiesHelper {

    private static final String CLIENT_NAME_PROPERTY_KEY = "clientName";
    private static final String CLIENT_NAME_PROPERTY_DEFAULT_VALUE = "Default clientName";
    private static final String ARTIFACT_ID = "configservice.artifactid";

    public static String getStringProperty(final Properties properties, String propertyKey, String defaultValue) {
        String property = properties.getProperty(propertyKey, defaultValue);
        if (property == null) {
            property = System.getProperty(propertyKey);
        }
        return property;
    }

    public static Integer getIntProperty(final Properties properties, String propertyKey, Integer defaultValue) {
        String property = getStringProperty(properties, propertyKey, null);
        if (property == null) {
            return defaultValue;
        }
        return Integer.valueOf(property);
    }

    public static String getClientNameFromProperties(Properties properties) {
        return getStringProperty(properties, CLIENT_NAME_PROPERTY_KEY, CLIENT_NAME_PROPERTY_DEFAULT_VALUE);
    }

    public static String getArtifactId(Properties properties) {
        return getStringProperty(properties, ARTIFACT_ID, null);
    }
}
