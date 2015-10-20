package no.cantara.jau.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-09-20.
 */
public class ClientEnvironmentUtil {
    private static final Logger log = LoggerFactory.getLogger(ClientEnvironmentUtil.class);
    public static final String NETWORKINTERFACE = "networkinterface_";

    public static SortedMap<String, String> getClientEnvironment() {
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

        clientEnv.putAll(System.getenv());
        return clientEnv;     }
}
