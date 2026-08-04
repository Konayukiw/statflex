package com.konayuki.statflex.utils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public final class Connection {
    private Connection() {
    }

    public static void applyIfIgnoringCertificates(HttpURLConnection conn) throws Exception {
        if (Toggles.ignoreCertificates && conn instanceof HttpsURLConnection) {
            trustAllCertificates((HttpsURLConnection) conn);
        }
    }

    public static void trustAllCertificates(HttpsURLConnection conn) throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new SecureRandom());
        conn.setSSLSocketFactory(sslContext.getSocketFactory());
        conn.setHostnameVerifier((hostname, session) -> true);
    }
}
