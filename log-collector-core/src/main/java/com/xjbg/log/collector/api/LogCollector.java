package com.xjbg.log.collector.api;

import com.xjbg.log.collector.model.LogInfo;

import java.util.Date;
import java.util.List;

/**
 * @author kesc
 * @since 2023-03-30 9:27
 */
public interface LogCollector<T extends LogInfo> {

    /**
     * collector type name e.g. database
     *
     * @return type
     */
    String type();

    /**
     * log in sync mode
     *
     * @param logInfo log info
     * @return true/false
     */
    boolean log(T logInfo);

    /**
     * log batch in sync mode
     *
     * @param logInfos log info
     * @return true/false
     */
    boolean logBatch(List<T> logInfos);

    /**
     * log in async mode
     *
     * @param logInfo log info
     */
    void logAsync(T logInfo);

    /**
     * clean log
     *
     * @param before clean logs before the date
     * @throws Exception e
     */
    void cleanLog(Date before) throws Exception;

    /**
     * start this collector
     */
    void start();

    /**
     * stop this collector
     */
    void stop();

    /**
     * set a fallback collector to use when log occur error or channel is full
     *
     * @param fallbackCollector fallback collector
     */
    default void setFallbackCollector(String fallbackCollector) {

    }

}
