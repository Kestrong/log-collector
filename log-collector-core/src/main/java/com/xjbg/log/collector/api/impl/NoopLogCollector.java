package com.xjbg.log.collector.api.impl;

import com.xjbg.log.collector.enums.CollectorType;
import com.xjbg.log.collector.model.LogInfo;

import java.util.Collections;
import java.util.List;

/**
 * @author kesc
 * @since 2023-03-30 12:13
 */
public class NoopLogCollector extends AbstractLogCollector<LogInfo, LogInfo> {

    @Override
    protected void init() {

    }

    @Override
    public String type() {
        return CollectorType.NOOP.getType();
    }

    @Override
    protected void doLog(List<LogInfo> logInfos) {
        log.debug("{} log collector todo nothing.", type());
    }

    @Override
    public void log(LogInfo logInfo) {
        doLog(Collections.singletonList(logInfo));
    }

    @Override
    public void logAsync(LogInfo logInfo) {
        log(logInfo);
    }

    @Override
    public void start() {
        start = true;
    }

    @Override
    public void stop() {
        start = false;
    }

}
