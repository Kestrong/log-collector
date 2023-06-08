package com.xjbg.log.collector.starter.autoconfig;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.xjbg.log.collector.LogCollectorConstant;
import com.xjbg.log.collector.LogCollectors;
import com.xjbg.log.collector.api.LogCollector;
import com.xjbg.log.collector.api.impl.*;
import com.xjbg.log.collector.channel.Channel;
import com.xjbg.log.collector.model.LogInfo;
import com.xjbg.log.collector.properties.LogCollectorProperties;
import com.xjbg.log.collector.request.LogCollectorGlobalFilter;
import com.xjbg.log.collector.retriever.UserIdRetriever;
import com.xjbg.log.collector.spring.utils.CompositeRexAntPathMatcher;
import com.xjbg.log.collector.spring.utils.LogCollectorCleaner;
import com.xjbg.log.collector.spring.utils.LogHttpUtil;
import com.xjbg.log.collector.starter.configuration.*;
import com.xjbg.log.collector.starter.filter.LogCollectorGatewayGlobalFilter;
import com.xjbg.log.collector.starter.filter.LogCollectorReactiveGlobalFilter;
import com.xjbg.log.collector.token.IHttpTokenCreator;
import com.xjbg.log.collector.transformer.LogTransformer;
import com.xjbg.log.collector.utils.JsonLogUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.StringUtils;
import org.springframework.web.server.WebFilter;

import javax.servlet.Filter;
import javax.sql.DataSource;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author kesc
 * @since 2023-04-04 16:38
 */
@Slf4j
@Import(value = {LogCollectorAopConfiguration.class,
        Es8LogCollectorConfiguration.class,
        Es7LogCollectorConfiguration.class,
        LogCollectorRefreshConfiguration.class,
        LogCollectorFeignConfiguration.class})
@AutoConfigureOrder(value = Integer.MAX_VALUE)
@Configuration
@SuppressWarnings(value = {"unchecked", "rawtypes"})
public class LogCollectorAutoConfiguration implements ApplicationRunner {
    private final LogCollectorProperties properties;
    private final ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Map<String, LogCollector> collectors = applicationContext.getBeansOfType(LogCollector.class);
        collectors.values().forEach(x -> LogCollectors.register(x.type(), x));
    }

    @JsonFilter("logCollectorFilter")
    static class DynamicFilter {

    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void refresh() {
        if (StringUtils.hasText(properties.getIgnoreProperties())) {
            Set<String> mustFields = BeanMap.create(LogInfo.builder().build()).keySet();
            Set<String> ignoreFields = new HashSet<>(Arrays.asList(properties.getIgnoreProperties().split(",")));
            ignoreFields.removeAll(mustFields);
            JsonLogUtil.getDefaultObjectMapper().setFilterProvider(new SimpleFilterProvider().addFilter("logCollectorFilter", SimpleBeanPropertyFilter.serializeAllExcept(ignoreFields)));
            JsonLogUtil.getDefaultObjectMapper().addMixIn(Object.class, DynamicFilter.class);
        }
        if (StringUtils.hasText(properties.getDefaultCollectorType())) {
            LogCollectors.setDefaultCollectorType(properties.getDefaultCollectorType());
        }
        if (StringUtils.hasText(properties.getApplication())) {
            LogCollectorConstant.APPLICATION = properties.getApplication();
        } else {
            String application = applicationContext.getEnvironment().getProperty("spring.application.name");
            if (StringUtils.hasText(application)) {
                LogCollectorConstant.APPLICATION = application;
            }
        }
    }

    public LogCollectorAutoConfiguration(LogCollectorProperties properties, ApplicationContext applicationContext) {
        this.properties = properties;
        this.applicationContext = applicationContext;
        refresh();
    }

    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingClass(value = "org.springframework.cloud.gateway.filter.GlobalFilter")
    @ConditionalOnClass(value = {FilterRegistrationBean.class, Filter.class})
    @Configuration
    public class LogCollectorGlobalFilterConfiguration {

        @Bean(name = "logCollectorGlobalFilter")
        @RefreshScope
        @ConditionalOnMissingBean(name = "logCollectorGlobalFilter")
        @ConditionalOnProperty(name = LogCollectorProperties.PREFIX + ".filter.enable", havingValue = "true")
        public LogCollectorGlobalFilter logCollectorGlobalFilter() throws ReflectiveOperationException {
            LogCollectorGlobalFilter logCollectorGlobalFilter = new LogCollectorGlobalFilter(properties);
            if (StringUtils.hasText(properties.getFilter().getUserIdRetriever())) {
                logCollectorGlobalFilter.setUserIdRetriever((UserIdRetriever) getBean(properties.getFilter().getUserIdRetriever()));
                if (StringUtils.hasText(properties.getFilter().getUserPropertyName())) {
                    logCollectorGlobalFilter.getUserIdRetriever().setUserPropertyName(properties.getFilter().getUserPropertyName());
                }
            }
            logCollectorGlobalFilter.setPathMatcher(new CompositeRexAntPathMatcher());
            return logCollectorGlobalFilter;
        }

        @Bean(name = "logCollectorGlobalFilterRegistrationBean")
        @RefreshScope
        @ConditionalOnMissingBean(name = "logCollectorGlobalFilterRegistrationBean")
        @ConditionalOnProperty(name = LogCollectorProperties.PREFIX + ".filter.enable", havingValue = "true")
        public FilterRegistrationBean logCollectorGlobalFilterRegistrationBean() throws ReflectiveOperationException {
            FilterRegistrationBean<LogCollectorGlobalFilter> registration = new FilterRegistrationBean();
            registration.setFilter(logCollectorGlobalFilter());
            registration.addUrlPatterns("/*");
            registration.setOrder(properties.getFilter().getOrder());
            registration.setName("requestIdFilter");
            return registration;
        }

    }

    @ConditionalOnClass(value = {GlobalFilter.class})
    @Configuration
    public class LogCollectorGatewayGlobalFilterConfiguration {

        @Bean(name = "logCollectorGlobalFilter")
        @RefreshScope
        @ConditionalOnMissingBean(name = "logCollectorGlobalFilter")
        @ConditionalOnProperty(name = LogCollectorProperties.PREFIX + ".filter.enable", havingValue = "true")
        public LogCollectorGatewayGlobalFilter logCollectorGlobalFilter() throws ReflectiveOperationException {
            LogCollectorGatewayGlobalFilter logCollectorGlobalFilter = new LogCollectorGatewayGlobalFilter(properties);
            if (StringUtils.hasText(properties.getFilter().getUserIdRetriever())) {
                logCollectorGlobalFilter.setUserIdRetriever((UserIdRetriever) getBean(properties.getFilter().getUserIdRetriever()));
                if (StringUtils.hasText(properties.getFilter().getUserPropertyName())) {
                    logCollectorGlobalFilter.getUserIdRetriever().setUserPropertyName(properties.getFilter().getUserPropertyName());
                }
            }
            logCollectorGlobalFilter.setPathMatcher(new CompositeRexAntPathMatcher());
            return logCollectorGlobalFilter;
        }

    }

    @ConditionalOnClass(value = {WebFilter.class})
    @ConditionalOnMissingClass(value = "org.springframework.cloud.gateway.filter.GlobalFilter")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @Configuration
    public class LogCollectorReactiveGlobalFilterConfiguration {

        @Bean(name = "logCollectorGlobalFilter")
        @RefreshScope
        @ConditionalOnMissingBean(name = "logCollectorGlobalFilter")
        @ConditionalOnProperty(name = LogCollectorProperties.PREFIX + ".filter.enable", havingValue = "true")
        public LogCollectorReactiveGlobalFilter logCollectorGlobalFilter() throws ReflectiveOperationException {
            LogCollectorReactiveGlobalFilter logCollectorGlobalFilter = new LogCollectorReactiveGlobalFilter(properties);
            if (StringUtils.hasText(properties.getFilter().getUserIdRetriever())) {
                logCollectorGlobalFilter.setUserIdRetriever((UserIdRetriever) getBean(properties.getFilter().getUserIdRetriever()));
                if (StringUtils.hasText(properties.getFilter().getUserPropertyName())) {
                    logCollectorGlobalFilter.getUserIdRetriever().setUserPropertyName(properties.getFilter().getUserPropertyName());
                }
            }
            logCollectorGlobalFilter.setPathMatcher(new CompositeRexAntPathMatcher());
            return logCollectorGlobalFilter;
        }

    }


    private Object getBean(String beanName) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        try {
            return applicationContext.getBean(beanName);
        } catch (Exception e) {
            log.error(e.getMessage());
            try {
                return applicationContext.getBean(Class.forName(beanName));
            } catch (Exception ee) {
                log.error(ee.getMessage());
                return Class.forName(beanName).newInstance();
            }
        }
    }

    public void setCustomProperties(AbstractLogCollector
                                            abstractLogCollector, LogCollectorProperties.LogCollectorCustomProperties customProperties) throws
            ReflectiveOperationException {
        if (StringUtils.hasText(customProperties.getFallbackCollector())) {
            abstractLogCollector.setFallbackCollector(customProperties.getFallbackCollector());
        }
        if (customProperties.getBatchSize() != null && customProperties.getBatchSize() > 0) {
            abstractLogCollector.setBatchSize(customProperties.getBatchSize());
        }
        if (customProperties.getPoolSize() != null && customProperties.getPoolSize() > 0) {
            abstractLogCollector.setPoolSize(customProperties.getPoolSize());
        }
        if (customProperties.getRejectPolicy() != null) {
            abstractLogCollector.setRejectPolicy(customProperties.getRejectPolicy());
        }
        if (StringUtils.hasText(customProperties.getLogTransformerImpl())) {
            abstractLogCollector.setLogTransformer((LogTransformer) getBean(customProperties.getLogTransformerImpl()));
            abstractLogCollector.getLogTransformer().afterProperties(customProperties.getProperties());
        }
        LogCollectorProperties.LogCollectorChannelProperties channelProperties = customProperties.getChannel();
        if (channelProperties != null) {
            if (StringUtils.hasText(channelProperties.getChannelClass())) {
                abstractLogCollector.setChannel((Channel) Class.forName(channelProperties.getChannelClass()).newInstance());
                abstractLogCollector.getChannel().afterProperties(customProperties.getProperties());
            }
            if (channelProperties.getByteCapacity() != null) {
                abstractLogCollector.getChannel().setByteCapacity(channelProperties.getByteCapacity());
            }
            if (channelProperties.getCapacity() != null) {
                abstractLogCollector.getChannel().setCapacity(channelProperties.getCapacity());
            }
            if (channelProperties.getByteSpeed() != null) {
                abstractLogCollector.getChannel().setByteSpeed(channelProperties.getByteSpeed());
            }
            if (channelProperties.getRecordSpeed() != null) {
                abstractLogCollector.getChannel().setRecordSpeed(channelProperties.getRecordSpeed());
            }
            if (channelProperties.getFlowControlInterval() != null) {
                abstractLogCollector.getChannel().setFlowControlInterval(channelProperties.getFlowControlInterval());
            }
            if (channelProperties.getThreshold() != null && channelProperties.getThreshold() > 0) {
                if (channelProperties.getThreshold() < 0.5f || channelProperties.getThreshold() > 0.9f) {
                    log.warn("remain threshold is suggested [0.5,0.9], you set {}", channelProperties.getThreshold());
                }
                abstractLogCollector.getChannel().setThreshold(Math.min(channelProperties.getThreshold(), 1.0f));
            }
        }
    }

    public void configure(ObjectMapper objectMapper, LogCollectorProperties.LogCollectorJsonProperties
            jsonProperties) {
        if (jsonProperties != null && objectMapper != null) {
            if (StringUtils.hasText(jsonProperties.getDateFormat())) {
                objectMapper.setDateFormat(new SimpleDateFormat(jsonProperties.getDateFormat()));
            }
            if (jsonProperties.getNamingStrategy() != null) {
                switch (jsonProperties.getNamingStrategy()) {
                    case SNAKE_CASE:
                        try {
                            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
                        } catch (Error e) {
                            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
                        }
                        break;
                    case UPPER_CAMEL_CASE:
                        try {
                            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE);
                        } catch (Error e) {
                            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
                        }
                        break;
                    case UPPER_SNAKE_CASE:
                        try {
                            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_SNAKE_CASE);
                        } catch (Error e) {
                            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
                        }
                        break;
                    case KEBAB_CASE:
                        try {
                            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
                        } catch (Error e) {
                            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
                        }
                        break;
                    default:
                        try {
                            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
                        } catch (Error e) {
                            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);
                        }
                }
            }
        }
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(name = LogCollectorProperties.PREFIX + ".enable", havingValue = "true")
    @ConditionalOnMissingBean(value = NoopLogCollector.class)
    public NoopLogCollector noopLogCollector() {
        return new NoopLogCollector();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(name = LogCollectorProperties.PREFIX + ".enable", havingValue = "true")
    @ConditionalOnMissingBean(value = CommonLogCollector.class)
    public CommonLogCollector commonLogCollector() {
        return new CommonLogCollector();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean(value = DataBaseLogCollector.class)
    @ConditionalOnProperty(name = LogCollectorProperties.PREFIX + ".database.enable", havingValue = "true")
    public DataBaseLogCollector dataBaseLogCollector(DataSource logCollectorDataSource) throws
            ReflectiveOperationException {
        DataBaseLogCollector dataBaseLogCollector = new DataBaseLogCollector(logCollectorDataSource);
        setCustomProperties(dataBaseLogCollector, properties.getDatabase());
        if (StringUtils.hasText(properties.getDatabase().getWrapper())) {
            dataBaseLogCollector.setWrapper(properties.getDatabase().getWrapper());
        }
        if (StringUtils.hasText(properties.getDatabase().getTableName())) {
            dataBaseLogCollector.setTableName(properties.getDatabase().getTableName());
        }
        return dataBaseLogCollector;
    }

    @Configuration
    @ConditionalOnClass(value = {HttpClient.class})
    public class HttpLogCollectorConfiguration {

        @Bean(initMethod = "start", destroyMethod = "stop")
        @ConditionalOnMissingBean(value = HttpLogCollector.class)
        @ConditionalOnProperty(name = LogCollectorProperties.PREFIX + ".http.enable", havingValue = "true")
        public HttpLogCollector httpLogCollector() throws ReflectiveOperationException {
            HttpLogCollector httpLogCollector = new HttpLogCollector(properties.getHttp().getUrl(), LogHttpUtil.createHttpClient(properties.getHttp().getConnection()));
            setCustomProperties(httpLogCollector, properties.getHttp());
            ObjectMapper objectMapper = JsonLogUtil.createObjectMapper();
            configure(objectMapper, properties.getHttp().getJson());
            httpLogCollector.setObjectMapper(objectMapper);
            if (StringUtils.hasText(properties.getHttp().getCharset())) {
                httpLogCollector.setCharset(properties.getHttp().getCharset());
            }
            if (StringUtils.hasText(properties.getHttp().getMethod())) {
                httpLogCollector.setMethod(properties.getHttp().getMethod());
            }
            if (StringUtils.hasText(properties.getHttp().getTokenHeaderCreator())) {
                httpLogCollector.setTokenCreator((IHttpTokenCreator) getBean(properties.getHttp().getTokenHeaderCreator()));
                httpLogCollector.getTokenCreator().afterProperties(properties.getHttp().getProperties());
            }
            return httpLogCollector;
        }

    }

    @Bean(name = "logCollectorCleaner")
    @ConditionalOnMissingBean(name = "logCollectorCleaner")
    @ConditionalOnProperty(name = LogCollectorProperties.PREFIX + ".clean-up", havingValue = "true")
    public LogCollectorCleaner logCollectorCleaner() {
        return new LogCollectorCleaner(properties);
    }

}
