package com.xjbg.log.collector.enums;

/**
 * @author kesc
 * @since 2023-04-04 9:50
 */
public enum CollectorType {
    NOOP,
    COMMON,
    DATABASE,
    HTTP,
    TCP,
    ES,
    REDIS,
    MQ,
    FEIGN,
    FILE,
    OTHER;

    public String getType() {
        return name().toLowerCase();
    }
}
