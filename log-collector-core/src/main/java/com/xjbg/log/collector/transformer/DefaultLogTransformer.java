package com.xjbg.log.collector.transformer;

import com.xjbg.log.collector.model.LogInfo;
import com.xjbg.log.collector.utils.JsonLogUtil;

import java.util.Map;

/**
 * @author kesc
 * @since 2023-03-30 16:36
 */
public class DefaultLogTransformer<T extends LogInfo, R> implements LogTransformer<T, R> {

    @Override
    public R transform(T logInfo) {
        logInfo.setParams(JsonLogUtil.toJson(logInfo.getParams()));
        logInfo.setResponse(JsonLogUtil.toJson(logInfo.getResponse()));
        return (R) logInfo;
    }

    @Override
    public void afterProperties(Map<String, String> properties) {

    }
}
