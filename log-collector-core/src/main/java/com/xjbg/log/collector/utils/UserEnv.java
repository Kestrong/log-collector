package com.xjbg.log.collector.utils;

/**
 * @author kesc
 * @see LogContextHolder
 * @since 2023-04-04 11:08
 */
@Deprecated
public class UserEnv {
    private static final ThreadLocal<String> userLocal = new InheritableThreadLocal<>();

    public static void setUser(String userId) {
        userLocal.set(userId);
    }


    public static String getUser() {
        return userLocal.get();
    }

    public static void remove() {
        userLocal.remove();
    }
}
