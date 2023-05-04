package com.xjbg.log.collector.api.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjbg.log.collector.model.LogInfo;
import com.xjbg.log.collector.utils.JsonLogUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

/**
 * @author kesc
 * @since 2023-04-10 15:09
 */
@Getter
@Setter
public abstract class AbstractJsonLogCollector<T extends LogInfo, R> extends AbstractLogCollector<T, R> {
    private ObjectMapper objectMapper;

    @SneakyThrows
    protected String toJsonString(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof String) {
            return (String) o;
        }
        if (getObjectMapper() == null) {
            return JsonLogUtil.toJson(o);
        }
        return getObjectMapper().writeValueAsString(o);
    }
}
