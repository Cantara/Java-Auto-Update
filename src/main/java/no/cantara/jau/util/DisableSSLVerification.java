package no.cantara.jau.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * This class provides a way to disable TLS check for certain domains. Useful in the case where JAU is running inside
 * a network that uses a proxy etc. that intercepts TLS traffic. Inspired by https://stackoverflow.com/q/49301717
 */
public class DisableSSLVerification {
    private static final Logger log = LoggerFactory.getLogger(DisableSSLVerification.class);

    static class CustomTrustManager implements X509TrustManager {

        /*
         * The default X509TrustManager returned by SunX509. We'll delegate decisions to
         * it, and fall back to the logic in this class if the default X509TrustManager
         * doesn't trust it.
         */
        private X509TrustManager sunJSSEX509TrustManager;
        private List<String> domains;

        public CustomTrustManager(List<String> domains) throws NoSuchAlgorithmException, KeyStoreException {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());

            trustManagerFactory.init((KeyStore) null);

            sunJSSEX509TrustManager = null;
            for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
                if (trustManager instanceof X509TrustManager) {
                    sunJSSEX509TrustManager = (X509TrustManager) trustManager;
                }
            }

            this.domains = domains;
        }

        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return sunJSSEX509TrustManager.getAcceptedIssuers();
        }

        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
                                       String paramString) throws CertificateException {
            sunJSSEX509TrustManager.checkClientTrusted(paramArrayOfX509Certificate, paramString);
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] paramArrayOfX509Certificate,
                                       String paramString) throws CertificateException {
            try {
                sunJSSEX509TrustManager.checkServerTrusted(paramArrayOfX509Certificate, paramString);
            } catch (CertificateException e) {
                for (X509Certificate a : paramArrayOfX509Certificate) {
                    for (String domain : domains) {
                        if (("CN=" + domain).equals(a.getSubjectDN().getName())) {
                            log.trace("Certificate for " + a.getSubjectDN().getName()
                                    + " accepted, although Java TrustManager did not accept it");
                            return;
                        }
                    }
                }
                throw new CertificateException("Certificate verification finally failed", e);
            }
        }
    }

    /**
     * Disables certificate validation for any HTTPS request by this application to given domains
     *
     * @param domains list of domains to ignore TLS certificate validation of. Note that each domain given is checked
     *                against subject DN of certificate, and as such if a wildcard certificate is expected at the domain
     *                to ignore, it must be specified as e.g. "*.somedomain.com"
     */
    public static void disableForDomains(List<String> domains) {
        log.warn("Overriding TLS certificate verification. Note that this means any certificate for the given " +
                "domains are accepted even if a certificate is not trusted by the JVM! Be careful and only use if " +
                "absolutely necessary! domains: {}", domains);
        TrustManager[] trustAllCerts;
        try {
            trustAllCerts = new TrustManager[]{new CustomTrustManager(domains)};
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new RuntimeException("Failed to create a trustmanager", e);
        }

        // Install the all-trusting trust manager
        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to install new trust store", e);
        }
        HostnameVerifier allHostsValid = (hostname, session) -> {
            for (String domain : domains) {
                if (hostname.equals(domain)) {
                    log.info("Override hostname verification for: " + hostname);
                    return true;
                }
            }
            return false;
        };
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }
}
