package no.cantara.jau.util;

import java.util.Properties;

public class ProxyFixer {

    public static void fixProxy(Properties properties) {
        Boolean useProxy = getBooleanProperty(properties, "http.useProxy", false);
        if (useProxy) {
            setProxy(properties);
        }
    }

    private static Boolean getBooleanProperty(final Properties properties, String propertyKey, Boolean defaultValue) {
        String property = getStringProperty(properties, propertyKey, null);
        if (property == null) {
            return defaultValue;
        }
        return Boolean.valueOf(property);
    }

    private static String getStringProperty(final Properties properties, String propertyKey, String defaultValue) {
        String property = properties.getProperty(propertyKey, defaultValue);
        if (property == null) {
            property = System.getProperty(propertyKey);
        }
        return property;
    }

    public static void setProxy(Properties properties) {
        System.setProperty("http.proxyHost", getHTTPHost(properties));
        System.setProperty("http.proxyPort", getHTTPPort(properties));
        System.setProperty("https.proxyHost", getHTTPSHost(properties));
        System.setProperty("https.proxyPort", getHTTPSPort(properties));

    }

    private static String getHTTPPort(Properties properties) {
        return getStringProperty(properties, "http.proxyPort", null);
    }

    private static String getHTTPHost(Properties properties) {
        return getStringProperty(properties, "http.proxyHost", null);
    }

    private static String getHTTPSHost(Properties properties) {
        return getStringProperty(properties, "https.proxyHost", null);
    }

    private static String getHTTPSPort(Properties properties) {
        return getStringProperty(properties, "https.proxyPort", null);
    }

}
