package com.xjbg.log.collector.utils;

import com.xjbg.log.collector.model.LogContext;

import java.util.Optional;

/**
 * @author kesc
 * @since 2023-07-24 10:09
 */
public class LogContextHolder {
    private static final ThreadLocal<LogContext> logContextThreadLocal = new InheritableThreadLocal<>();

    public static void setContext(LogContext logContext) {
        logContextThreadLocal.set(logContext);
    }

    public static Optional<LogContext> getContext() {
        return Optional.ofNullable(logContextThreadLocal.get());
    }

    public static void remove() {
        logContextThreadLocal.remove();
    }
}
