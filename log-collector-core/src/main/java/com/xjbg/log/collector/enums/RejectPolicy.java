package com.xjbg.log.collector.enums;

/**
 * @author kesc
 * @since 2023-03-30 17:17
 */
public enum RejectPolicy {

    NOOP,
    FALLBACK,
    CALLER_RUNS,
    DISCARD,
    DISCARD_OLDEST;

}
