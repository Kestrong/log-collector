package com.xjbg.log.collector.api.impl;

import com.xjbg.log.collector.enums.CollectorType;
import com.xjbg.log.collector.enums.LogState;
import com.xjbg.log.collector.model.LogInfo;

import java.util.List;

/**
 * @author kesc
 * @since 2023-03-30 12:13
 */
public class CommonLogCollector extends NoopLogCollector {

    @Override
    public String type() {
        return CollectorType.COMMON.getType();
    }

    @Override
    protected void doLog(List<LogInfo> logInfos) {
        logInfos.forEach(logInfo -> {
            completeLogInfo(logInfo);
            if (LogState.FAIL.name().equalsIgnoreCase(logInfo.getState())) {
                log.error("{}", logInfo);
            } else {
                log.info("{}", logInfo);
            }
        });
    }

}