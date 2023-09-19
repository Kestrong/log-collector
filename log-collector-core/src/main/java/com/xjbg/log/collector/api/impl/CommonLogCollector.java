package com.xjbg.log.collector.api.impl;

import com.xjbg.log.collector.LogCollectorConstant;
import com.xjbg.log.collector.enums.CollectorType;
import com.xjbg.log.collector.enums.LogState;
import com.xjbg.log.collector.model.LogInfo;
import com.xjbg.log.collector.transformer.DefaultLogTransformer;
import org.slf4j.MDC;

import java.util.List;

/**
 * @author kesc
 * @since 2023-03-30 12:13
 */
public class CommonLogCollector extends AbstractLogCollector<LogInfo, Object> {

    @Override
    public String type() {
        return CollectorType.COMMON.getType();
    }


    protected void doLog(List<Object> logInfos) {
        //do nothing
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean log(LogInfo logInfo) {
        try {
            MDC.put(LogCollectorConstant.BUSINESS_NO, logInfo.getBusinessNo());
            Object transform = doTransform(completeLogInfo(logInfo));
            if (LogState.FAIL.name().equalsIgnoreCase(logInfo.getState())) {
                log.error("{}", transform);
            } else {
                log.info("{}", transform);
            }
            return true;
        } catch (Exception e) {
            log.warn("log occur error, the reason maybe: {}", e.getMessage());
            logAsyncFallback(logInfo);
            return false;
        } finally {
            MDC.remove(LogCollectorConstant.BUSINESS_NO);
            if (getNextCollector() != null) {
                getNextCollector().log(logInfo);
            }
        }
    }

    @Override
    public boolean logBatch(List<LogInfo> logInfos) {
        logInfos.forEach(this::log);
        return true;
    }

}