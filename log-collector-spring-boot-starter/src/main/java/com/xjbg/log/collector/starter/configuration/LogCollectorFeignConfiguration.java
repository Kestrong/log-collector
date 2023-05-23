package com.xjbg.log.collector.starter.configuration;

import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.xjbg.log.collector.properties.LogCollectorProperties;
import com.xjbg.log.collector.starter.autoconfig.LogCollectorAutoConfiguration;
import com.xjbg.log.collector.starter.feign.FeignLogCollector;
import com.xjbg.log.collector.starter.feign.FeignLogCollectorClient;
import com.xjbg.log.collector.starter.feign.LogCollectorHystrixConcurrencyStrategy;
import com.xjbg.log.collector.starter.feign.LogCollectorRequestInterceptor;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * @author kesc
 * @since 2023-04-19 8:59
 */
@ConditionalOnClass(value = {RequestTemplate.class, RequestInterceptor.class})
@Configuration
public class LogCollectorFeignConfiguration {
    @Autowired
    private LogCollectorProperties properties;

    @RefreshScope
    @Bean(value = "logCollectorRequestInterceptor")
    @ConditionalOnProperty(name = LogCollectorProperties.PREFIX + ".filter.enable", havingValue = "true")
    @ConditionalOnMissingBean(name = "logCollectorRequestInterceptor")
    public LogCollectorRequestInterceptor logCollectorRequestInterceptor() {
        return new LogCollectorRequestInterceptor(properties);
    }

    @Configuration
    @ConditionalOnClass(value = {HystrixConcurrencyStrategy.class})
    protected static class LogCollectorHystrixConfiguration {

        @Bean(value = "logCollectorHystrixConcurrencyStrategy")
        @ConditionalOnProperty(name = LogCollectorProperties.PREFIX + ".filter.enable", havingValue = "true")
        @ConditionalOnMissingBean(name = "logCollectorHystrixConcurrencyStrategy")
        public LogCollectorHystrixConcurrencyStrategy logCollectorHystrixConcurrencyStrategy() {
            return new LogCollectorHystrixConcurrencyStrategy();
        }

    }

    @Configuration
    @ConditionalOnClass(value = {FeignClient.class, FeignClientBuilder.class})
    public static class FeignLogCollectorConfiguration {
        @Autowired
        private LogCollectorAutoConfiguration logCollectorAutoConfiguration;
        @Autowired
        private LogCollectorProperties properties;

        @Bean(value = "feignLogCollector", initMethod = "start", destroyMethod = "stop")
        @ConditionalOnProperty(name = LogCollectorProperties.PREFIX + ".feign.enable", havingValue = "true")
        @ConditionalOnMissingBean(name = "feignLogCollector")
        public FeignLogCollector feignLogCollector(FeignLogCollectorClient feignLogCollectorClient) throws ReflectiveOperationException {
            FeignLogCollector feignLogCollector = new FeignLogCollector(feignLogCollectorClient);
            LogCollectorProperties.FeignLogCollectorCustomProperties propertiesFeign = properties.getFeign();
            logCollectorAutoConfiguration.setCustomProperties(feignLogCollector, propertiesFeign);
            if (StringUtils.hasText(propertiesFeign.getMethod())) {
                feignLogCollector.setMethod(propertiesFeign.getMethod());
            }
            return feignLogCollector;
        }

        @Bean(name = "feignLogCollectorClient")
        @ConditionalOnMissingBean(name = "feignLogCollectorClient")
        @ConditionalOnProperty(name = LogCollectorProperties.PREFIX + ".feign.enable", havingValue = "true")
        public FeignLogCollectorClient feignLogCollectorClient() {
            FeignClientBuilder.Builder<FeignLogCollectorClient> builder = new FeignClientBuilder(logCollectorAutoConfiguration.getApplicationContext())
                    .forType(FeignLogCollectorClient.class, properties.getFeign().getName())
                    .contextId("feignLogCollectorClientInner")
                    .url(properties.getFeign().getUrl());
            return builder.build();
        }

    }

}
