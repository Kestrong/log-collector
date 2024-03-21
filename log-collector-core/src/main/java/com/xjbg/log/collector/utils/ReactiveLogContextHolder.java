package com.xjbg.log.collector.utils;

import com.xjbg.log.collector.model.LogContext;
import reactor.core.publisher.Mono;

/**
 * @author kesc
 * @since 2023-07-24 10:43
 */
@SuppressWarnings("deprecation")
public class ReactiveLogContextHolder {

    public static Mono<Void> setContext(Mono<Void> mono, LogContext logContext) {
        return mono.contextWrite(x -> {
            if (!x.hasKey(LogContext.class)) {
                return x.put(LogContext.class, logContext);
            }
            return x;
        });
    }

    public static Mono<LogContext> getContext() {
        return Mono.deferContextual(x -> {
            if (x.hasKey(LogContext.class)) {
                return Mono.just(x.get(LogContext.class));
            }
            return Mono.just(LogContext.builder().build());
        });
    }
}
