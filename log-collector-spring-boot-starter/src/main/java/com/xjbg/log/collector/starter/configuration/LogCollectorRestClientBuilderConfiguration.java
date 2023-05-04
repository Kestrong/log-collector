package com.xjbg.log.collector.starter.configuration;

import com.xjbg.log.collector.properties.LogCollectorProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;

/**
 * @author kesc
 * @since 2023-04-19 9:21
 */
@Slf4j
public abstract class LogCollectorRestClientBuilderConfiguration {

    protected RestClientBuilder logCollectorRestClientBuilder(LogCollectorProperties properties, Object[] sslContext) {
        LogCollectorProperties.EsLogCollectorCustomProperties propertiesEs = properties.getEs();
        String[] hostList = propertiesEs.getHosts().split(",");
        HttpHost[] httpHosts = new HttpHost[hostList.length];
        int i = 0;
        for (String sHost : hostList) {
            String[] host = sHost.split(":");
            httpHosts[i++] = new HttpHost(host[0], Integer.parseInt(host[1]), propertiesEs.getSchema());
        }
        LogCollectorProperties.LogCollectorConnectionProperties connectionProperties = propertiesEs.getConnection();
        RestClientBuilder restClientBuilder = RestClient.builder(httpHosts)
                .setRequestConfigCallback(requestConfigBuilder -> requestConfigBuilder
                        .setConnectionRequestTimeout(connectionProperties.getRequestTimeout())
                        .setConnectTimeout(connectionProperties.getConnectTimeout()).setSocketTimeout(connectionProperties.getSocketTimeout()))
                .setHttpClientConfigCallback((httpAsyncClientBuilder) -> {
                    httpAsyncClientBuilder.setMaxConnTotal(connectionProperties.getMaxConnect())
                            .setMaxConnPerRoute(connectionProperties.getMaxConnectPerRoute())
                            .setDefaultRequestConfig(RequestConfig.custom().setSocketTimeout(connectionProperties.getSocketTimeout())
                                    .setConnectTimeout(connectionProperties.getConnectTimeout())
                                    .setConnectionRequestTimeout(connectionProperties.getRequestTimeout()).build())
                            .setDefaultIOReactorConfig(IOReactorConfig.custom().setConnectTimeout(connectionProperties.getConnectTimeout())
                                    .setSoTimeout(connectionProperties.getSocketTimeout()).build());
                    if (sslContext != null) {
                        httpAsyncClientBuilder.setSSLContext((SSLContext) sslContext[0]);
                    }
                    if (StringUtils.hasText(propertiesEs.getUsername()) && StringUtils.hasText(propertiesEs.getPassword())) {
                        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
                                propertiesEs.getUsername(), propertiesEs.getPassword()));
                        httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                    return httpAsyncClientBuilder;
                });
        restClientBuilder.setFailureListener(new RestClient.FailureListener() {
            @Override
            public void onFailure(Node node) {
                log.error("es connect failure : {}", node.toString());
            }
        });
        return restClientBuilder;
    }
}
