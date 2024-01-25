package com.xjbg.log.collector.spring.utils;

import com.xjbg.log.collector.properties.LogCollectorProperties;
import lombok.SneakyThrows;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.util.StringUtils;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * @author kesc
 * @since 2023-04-23 9:52
 */
public class LogHttpUtil {

    @SneakyThrows
    public static Object[] ignoreSslContext() {
        SSLContext sc = SSLContext.getInstance("TLS");
        X509TrustManager x509TrustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };
        sc.init(null, new X509TrustManager[]{x509TrustManager}, new SecureRandom());
        return new Object[]{sc, x509TrustManager};
    }

    public static Object[] sslContext(String certFile) {
        if (!StringUtils.hasText(certFile)) {
            return null;
        }
        try {
            Collection<? extends Certificate> certificates = null;
            try (FileInputStream fis = new FileInputStream(certFile)) {
                certificates = CertificateFactory.getInstance("X.509").generateCertificates(fis);
            }

            if (certificates == null || certificates.isEmpty()) {
                throw new IllegalArgumentException("expected non-empty set of trusted certificates");
            }

            // Any password will work.
            char[] password = "password".toCharArray();
            // Put the certificates a key store.
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            // By convention, 'null' creates an empty key store.
            keyStore.load(null, password);

            int index = 0;
            for (Certificate certificate : certificates) {
                String certificateAlias = Integer.toString(index++);
                keyStore.setCertificateEntry(certificateAlias, certificate);
            }

            // Use it to build an X509 trust manager.
            KeyManagerFactory keyManagerFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, password);
            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            final KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();
            final TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, new SecureRandom());
            return new Object[]{sslContext, trustManagers[0]};
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    public static HttpClient createHttpClient(LogCollectorProperties.LogCollectorConnectionProperties connectionProperties) {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
                .setMaxConnTotal(connectionProperties.getMaxConnect())
                .setMaxConnPerRoute(connectionProperties.getMaxConnectPerRoute())
                .setDefaultRequestConfig(RequestConfig.custom().setSocketTimeout(connectionProperties.getSocketTimeout())
                        .setConnectTimeout(connectionProperties.getConnectTimeout())
                        .setConnectionRequestTimeout(connectionProperties.getRequestTimeout()).build());
        if (connectionProperties.isIgnoreHttps()) {
            httpClientBuilder.setSSLContext((SSLContext) ignoreSslContext()[0]).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        } else {
            Object[] objects = sslContext(connectionProperties.getCertFile());
            if (objects != null) {
                httpClientBuilder.setSSLContext((SSLContext) objects[0]).setSSLHostnameVerifier(SSLConnectionSocketFactory.getDefaultHostnameVerifier());
            }
        }
        return httpClientBuilder.build();
    }

}
