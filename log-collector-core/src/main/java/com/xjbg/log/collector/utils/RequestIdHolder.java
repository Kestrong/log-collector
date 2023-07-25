package com.xjbg.log.collector.utils;

/**
 * @author kesc
 * @see LogContextHolder
 * @since 2023-04-14 12:22
 */
@Deprecated
public class RequestIdHolder {
    private static final ThreadLocal<String> requestIdHolder = new InheritableThreadLocal<>();

    public static void setRequestId(String requestId) {
        requestIdHolder.set(requestId);
    }


    public static String getRequestId() {
        return requestIdHolder.get();
    }

    public static void remove() {
        requestIdHolder.remove();
    }
}
