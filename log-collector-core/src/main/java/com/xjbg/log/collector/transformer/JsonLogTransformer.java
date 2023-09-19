package com.xjbg.log.collector.transformer;

import com.xjbg.log.collector.model.LogInfo;
import com.xjbg.log.collector.utils.JsonLogUtil;

import java.util.Map;

/**
 * @author kesc
 * @since 2023-09-19 15:32
 */
public class JsonLogTransformer<T extends LogInfo> implements LogTransformer<T, String> {

    @Override
    public String transform(T logInfo) {
        return JsonLogUtil.toJson(logInfo);
    }

    @Override
    public void afterProperties(Map<String, String> properties) {

    }

}
