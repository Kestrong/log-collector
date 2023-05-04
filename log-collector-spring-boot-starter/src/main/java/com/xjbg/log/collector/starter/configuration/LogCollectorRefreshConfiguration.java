package com.xjbg.log.collector.starter.configuration;


import com.xjbg.log.collector.starter.autoconfig.LogCollectorAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.endpoint.event.RefreshEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

/**
 * @author kesc
 * @since 2023-04-19 12:29
 */
@ConditionalOnClass(value = {RefreshEvent.class})
@Configuration
@RefreshScope
public class LogCollectorRefreshConfiguration implements ApplicationListener<ApplicationEvent> {
    private static Long lastRefreshTimestamp = -1L;
    @Autowired
    private LogCollectorAutoConfiguration logCollectorAutoConfiguration;

    @Override
    public void onApplicationEvent(@NonNull ApplicationEvent event) {
        if (System.currentTimeMillis() - lastRefreshTimestamp < 5000L) {
            return;
        }
        try {
            if (event instanceof RefreshEvent) {
                logCollectorAutoConfiguration.refresh();
                lastRefreshTimestamp = System.currentTimeMillis();
            }
        } catch (Error e) {
            //ignore
        }
    }

}
