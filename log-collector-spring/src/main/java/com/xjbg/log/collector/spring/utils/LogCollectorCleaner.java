package com.xjbg.log.collector.spring.utils;

import com.xjbg.log.collector.LogCollectors;
import com.xjbg.log.collector.properties.LogCollectorProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.Date;

/**
 * @author kesc
 * @since 2023-04-19 10:02
 */
@Slf4j
public class LogCollectorCleaner implements InitializingBean, DisposableBean {
    private final ThreadPoolTaskScheduler scheduler;
    private final LogCollectorProperties properties;

    public LogCollectorCleaner(LogCollectorProperties properties) {
        this.properties = properties;
        this.scheduler = new ThreadPoolTaskScheduler();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setPoolSize(1);
        scheduler.initialize();
        //0 0 0 * * ? 一天一次 0 0/1 * * * ? 一分钟一次
        scheduler.schedule(() -> {
            if (properties.isCleanUp() && properties.getMaxHistory() > 0) {
                LogCollectors.getCollectors().forEach(x -> {
                    try {
                        x.cleanLog(DateUtils.addDays(new Date(), -properties.getMaxHistory()));
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                });
            }
        }, new CronTrigger("0 0 0 * * ?"));
    }

    @Override
    public void destroy() throws Exception {
        try {
            scheduler.shutdown();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

}
