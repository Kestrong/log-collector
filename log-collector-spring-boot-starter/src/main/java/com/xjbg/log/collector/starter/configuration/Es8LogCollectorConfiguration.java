package com.xjbg.log.collector.starter.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjbg.log.collector.api.impl.Es8LogCollector;
import com.xjbg.log.collector.properties.LogCollectorProperties;
import com.xjbg.log.collector.spring.utils.LogHttpUtil;
import com.xjbg.log.collector.starter.autoconfig.LogCollectorAutoConfiguration;
import com.xjbg.log.collector.utils.JsonLogUtil;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * @author kesc
 * @since 2023-04-18 17:45
 */
@ConditionalOnClass(value = {RestClientBuilder.class, ElasticsearchClient.class, JacksonJsonpMapper.class, RestClientTransport.class})
@Configuration
public class Es8LogCollectorConfiguration extends LogCollectorRestClientBuilderConfiguration {
    @Autowired
    private LogCollectorAutoConfiguration logCollectorAutoConfiguration;
    @Autowired
    private LogCollectorProperties properties;

    @Bean(name = "esLogCollector", initMethod = "start", destroyMethod = "stop")
    @ConditionalOnExpression(value = "${" + LogCollectorProperties.PREFIX + ".es.enable:false}&&'${" + LogCollectorProperties.PREFIX + ".es.version:7}'.equals('8')")
    @ConditionalOnMissingBean(name = "esLogCollector")
    public Es8LogCollector esLogCollector() throws ReflectiveOperationException {
        Es8LogCollector esLogCollector = new Es8LogCollector(logCollectorElasticsearchClient());
        LogCollectorProperties.EsLogCollectorCustomProperties propertiesEs = properties.getEs();
        logCollectorAutoConfiguration.setCustomProperties(esLogCollector, propertiesEs);
        if (StringUtils.hasText(propertiesEs.getIndex())) {
            esLogCollector.setIndex(propertiesEs.getIndex());
        }
        if (propertiesEs.getJson().getNamingStrategy() != null) {
            esLogCollector.setNamingStrategy(propertiesEs.getJson().getNamingStrategy());
        }
        return esLogCollector;
    }

    @Bean(name = "logCollectorElasticsearchClient")
    @ConditionalOnExpression(value = "${" + LogCollectorProperties.PREFIX + ".es.enable:false}&&'${" + LogCollectorProperties.PREFIX + ".es.version:7}'.equals('8')")
    @ConditionalOnMissingBean(name = "logCollectorElasticsearchClient")
    public ElasticsearchClient logCollectorElasticsearchClient() {
        ObjectMapper objectMapper = JsonLogUtil.createObjectMapper();
        logCollectorAutoConfiguration.configure(objectMapper, properties.getEs().getJson());
        JacksonJsonpMapper jacksonJsonpMapper = new JacksonJsonpMapper(objectMapper);
        Object[] sslContext = properties.getEs().getConnection().isIgnoreHttps() ? LogHttpUtil.ignoreSslContext() : LogHttpUtil.sslContext(properties.getEs().getConnection().getCertFile());
        RestClientTransport restClientTransport = new RestClientTransport(logCollectorRestClientBuilder(properties, sslContext).build(), jacksonJsonpMapper);
        return new ElasticsearchClient(restClientTransport);
    }

}
