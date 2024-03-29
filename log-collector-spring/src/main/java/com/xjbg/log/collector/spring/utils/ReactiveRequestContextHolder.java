package com.xjbg.log.collector.spring.utils;

import com.xjbg.log.collector.spring.model.ReactiveRequestContext;
import reactor.core.publisher.Mono;

/**
 * @author kesc
 * @since 2023-07-25 8:41
 */
public class ReactiveRequestContextHolder {
    public static Mono<Void> setRequestContext(Mono<Void> mono, ReactiveRequestContext requestContext) {
        try {
            return mono.contextWrite(x -> {
                if (!x.hasKey(ReactiveRequestContext.class)) {
                    return x.put(ReactiveRequestContext.class, requestContext);
                }
                return x;
            });
        } catch (Error e) {
            //compatibility with reactor version below 3.4
            return mono.subscriberContext(x -> {
                if (!x.hasKey(ReactiveRequestContext.class)) {
                    return x.put(ReactiveRequestContext.class, requestContext);
                }
                return x;
            });
        }
    }

    public static Mono<ReactiveRequestContext> getRequestContext() {
        try {
            return Mono.deferContextual(x -> {
                if (x.hasKey(ReactiveRequestContext.class)) {
                    return Mono.just(x.get(ReactiveRequestContext.class));
                }
                return Mono.just(ReactiveRequestContext.builder().build());
            });
        } catch (Error e) {
            //compatibility with reactor version below 3.4
            return Mono.subscriberContext().flatMap(x -> {
                if (x.hasKey(ReactiveRequestContext.class)) {
                    return Mono.just(x.get(ReactiveRequestContext.class));
                }
                return Mono.just(ReactiveRequestContext.builder().build());
            });
        }
    }
}
