package com.xjbg.log.collector.spring.utils;

import com.xjbg.log.collector.spring.model.ReactiveRequestContext;
import reactor.core.publisher.Mono;

/**
 * @author kesc
 * @since 2023-07-25 8:41
 */
public class ReactiveRequestContextHolder {
    public static Mono<Void> setRequestContext(Mono<Void> mono, ReactiveRequestContext requestContext) {
        return mono.contextWrite(x -> {
            if (!x.hasKey(ReactiveRequestContext.class)) {
                return x.put(ReactiveRequestContext.class, requestContext);
            }
            return x;
        });
    }

    public static Mono<ReactiveRequestContext> getRequestContext() {
        return Mono.deferContextual(x -> {
            if (x.hasKey(ReactiveRequestContext.class)) {
                return Mono.just(x.get(ReactiveRequestContext.class));
            }
            return Mono.just(ReactiveRequestContext.builder().build());
        });
    }
}
