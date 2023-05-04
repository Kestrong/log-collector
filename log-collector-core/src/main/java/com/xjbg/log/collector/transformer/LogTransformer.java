package com.xjbg.log.collector.transformer;

import com.xjbg.log.collector.model.LogInfo;

import java.util.Map;

/**
 * @author kesc
 * @since 2023-03-30 10:29
 */
public interface LogTransformer<T extends LogInfo, R> {

    R transform(T logInfo);

    void afterProperties(Map<String, String> properties);
}
