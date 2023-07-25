package com.xjbg.log.collector.starter.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjbg.log.collector.api.impl.Es7LogCollector;
import com.xjbg.log.collector.properties.LogCollectorProperties;
import com.xjbg.log.collector.spring.utils.LogHttpUtil;
import com.xjbg.log.collector.starter.autoconfig.LogCollectorAutoConfiguration;
import com.xjbg.log.collector.utils.JsonLogUtil;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
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
@Slf4j
@ConditionalOnClass(value = {RestClientBuilder.class, RestHighLevelClient.class, RestClientBuilder.class})
@Configuration
public class Es7LogCollectorConfiguration extends LogCollectorRestClientBuilderConfiguration {
    @Autowired
    private LogCollectorAutoConfiguration logCollectorAutoConfiguration;
    @Autowired
    private LogCollectorProperties properties;

    @Bean(value = "esLogCollector", initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean(name = "esLogCollector")
    @ConditionalOnExpression(value = "${" + LogCollectorProperties.PREFIX + ".es.enable:false}&&'${" + LogCollectorProperties.PREFIX + ".es.version:7}'.equals('7')")
    public Es7LogCollector esLogCollector(RestHighLevelClient logCollectorRestHighLevelClient) throws ReflectiveOperationException {
        LogCollectorProperties.EsLogCollectorCustomProperties propertiesEs = properties.getEs();
        Es7LogCollector esLogCollector = new Es7LogCollector(logCollectorRestHighLevelClient);
        ObjectMapper objectMapper = JsonLogUtil.createObjectMapper();
        logCollectorAutoConfiguration.configure(objectMapper, propertiesEs.getJson());
        esLogCollector.setObjectMapper(objectMapper);
        logCollectorAutoConfiguration.setCustomProperties(esLogCollector, propertiesEs);
        if (StringUtils.hasText(propertiesEs.getIndex())) {
            esLogCollector.setIndex(propertiesEs.getIndex());
        }
        if (StringUtils.hasText(propertiesEs.getIndex())) {
            esLogCollector.setIndex(propertiesEs.getIndex());
        }
        if (propertiesEs.getJson().getNamingStrategy() != null) {
            esLogCollector.setNamingStrategy(propertiesEs.getJson().getNamingStrategy());
        }
        return esLogCollector;
    }

    @Bean(name = "logCollectorRestHighLevelClient")
    @ConditionalOnMissingBean(value = RestHighLevelClient.class)
    @ConditionalOnExpression(value = "${" + LogCollectorProperties.PREFIX + ".es.enable:false}&&'${" + LogCollectorProperties.PREFIX + ".es.version:7}'.equals('7')")
    public RestHighLevelClient logCollectorRestHighLevelClient() {
        LogCollectorProperties.EsLogCollectorCustomProperties propertiesEs = properties.getEs();
        Object[] sslContext = propertiesEs.getConnection().isIgnoreHttps() ? LogHttpUtil.ignoreSslContext() : LogHttpUtil.sslContext(propertiesEs.getConnection().getCertFile());
        return new RestHighLevelClient(logCollectorRestClientBuilder(properties, sslContext));
    }

}

