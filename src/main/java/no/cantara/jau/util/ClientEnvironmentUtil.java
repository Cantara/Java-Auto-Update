package no.cantara.jau.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-09-20.
 */
public class ClientEnvironmentUtil {
    private static final Logger log = LoggerFactory.getLogger(ClientEnvironmentUtil.class);
    public static final String NETWORKINTERFACE = "networkinterface_";

    public static SortedMap<String, String> getClientEnvironment(Properties applicationState, String processIsRunning) {
        SortedMap<String, String> clientEnv = new TreeMap<>();

        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                for (InterfaceAddress interfaceAddress : nic.getInterfaceAddresses()) {
                    InetAddress address = interfaceAddress.getAddress();
                    if (address.isLoopbackAddress()) {
                        continue;
                    }

                    if (address.isSiteLocalAddress()) {
                        clientEnv.put(NETWORKINTERFACE + nic.getName(), address.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            log.warn("getNetworkInterfaces failed. Networkinterface addresses will not be availble.");
        }

        // We mask environment variables that are defined in appliction-env.properties, the assumption is that these are
        // sensitive and we do want to send them as heartbeat data
        Set<String> propertiesToMask = PropertiesHelper.getPropertiesFromConfigFile(PropertiesHelper.APPLICATION_ENV_FILENAME).stringPropertyNames();
        clientEnv.putAll(maskApplicationEnvProperties(System.getenv(), propertiesToMask));
        String version = PropertiesHelper.getVersion();
        clientEnv.put("jau.version", version);
        clientEnv.put("applicationState", String.valueOf(applicationState));
        clientEnv.put("processIsRunning", processIsRunning);
        clientEnv.put("processIsRunning timestamp", new Date().toString());
        return clientEnv;
    }

    public static SortedMap<String, String> getClientEnvironment() {
        return getClientEnvironment(new Properties(), "information not available");
    }

    static Map<String, String> maskApplicationEnvProperties(Map<String, String> environmentVariables, Set<String> variablesToMask) {
        HashMap<String, String> masked = new HashMap<>();
        for (String variableKey : environmentVariables.keySet()) {
            if (variablesToMask.contains(variableKey)) {
                masked.put(variableKey, maskEnvironmentVariable(environmentVariables.get(variableKey)));
            } else {
                masked.put(variableKey, environmentVariables.get(variableKey));
            }
        }
        return masked;
    }

    private static String maskEnvironmentVariable(String variableValue) {
        if (variableValue == null || variableValue.length() < 5) {
            // We assume short variable values are not sensitive
            return variableValue;
        }
        return variableValue.substring(0, 2) + "..." + variableValue.substring(variableValue.length() - 2, variableValue.length());
    }
}
