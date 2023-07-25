package com.xjbg.log.collector.starter.configuration;

import com.xjbg.log.collector.LogCollectors;
import com.xjbg.log.collector.api.LogCollector;
import com.xjbg.log.collector.starter.autoconfig.LogCollectorAutoConfiguration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * @author kesc
 * @since 2023-07-25 15:34
 */
@Configuration
@SuppressWarnings(value = {"rawtypes"})
public class LogCollectorRegisterConfiguration implements InitializingBean {
    @Autowired
    private LogCollectorAutoConfiguration logCollectorAutoConfiguration;

    @Override
    public void afterPropertiesSet() {
        Map<String, LogCollector> collectors = logCollectorAutoConfiguration.getApplicationContext().getBeansOfType(LogCollector.class);
        collectors.values().forEach(x -> LogCollectors.register(x.type(), x));
    }
}
