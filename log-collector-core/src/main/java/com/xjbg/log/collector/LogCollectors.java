package com.xjbg.log.collector;

import com.xjbg.log.collector.api.LogCollector;
import com.xjbg.log.collector.api.impl.CommonLogCollector;
import com.xjbg.log.collector.api.impl.NoopLogCollector;
import com.xjbg.log.collector.enums.CollectorType;

import java.util.*;

/**
 * @author kesc
 * @since 2023-03-30 9:43
 */
@SuppressWarnings(value = {"unused", "unchecked", "rawtypes"})
public class LogCollectors {
    private static String defaultCollectorType = CollectorType.COMMON.getType();
    private static final Map<String, LogCollector> COLLECTORS = new HashMap<>();

    static {
        register(CollectorType.NOOP.getType(), new NoopLogCollector());
        register(CollectorType.COMMON.getType(), new CommonLogCollector());
    }

    public static void setDefaultCollectorType(String type) {
        defaultCollectorType = type;
    }

    public static Collection<LogCollector> getCollectors() {
        return Collections.unmodifiableList(new ArrayList<>(COLLECTORS.values()));
    }

    public static void register(String type, LogCollector logCollector) {
        COLLECTORS.put(type, Objects.requireNonNull(logCollector, String.format("no such log collector of %s.", type)));
    }

    public static LogCollector getCollector(String type) {
        return Objects.requireNonNull(COLLECTORS.get(type), String.format("no such log collector of %s.", type));
    }

    public static <T> T getCollector(String type, Class<T> clazz) {
        return (T) getCollector(type);
    }

    public static LogCollector defaultCollector() {
        return getCollector(defaultCollectorType);
    }

    public static <T> T getCollector(Class<T> clazz) {
        for (LogCollector logCollector : COLLECTORS.values()) {
            if (clazz.isAssignableFrom(logCollector.getClass())) {
                return (T) logCollector;
            }
        }
        throw new NullPointerException(String.format("no such log collector of %s.", clazz.getName()));
    }

    public static void remove(String type) {
        COLLECTORS.remove(type);
    }
}
