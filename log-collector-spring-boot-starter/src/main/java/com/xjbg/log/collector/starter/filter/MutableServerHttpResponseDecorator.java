package com.xjbg.log.collector.starter.filter;

import com.xjbg.log.collector.utils.ByteUtil;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author kesc
 * @since 2023-04-23 18:15
 */
public class MutableServerHttpResponseDecorator extends ServerHttpResponseDecorator {
    private final List<byte[]> cachedBody;
    private final Predicate<String> canConsume;

    public MutableServerHttpResponseDecorator(ServerHttpResponse delegate, Predicate<String> canConsume) {
        super(delegate);
        this.canConsume = canConsume;
        this.cachedBody = new ArrayList<>();
    }

    public byte[] getContent() {
        return cachedBody.size() > 0 ? ByteUtil.mergeBytes(cachedBody) : null;
    }

    private boolean canConsume() {
        return canConsume != null && canConsume.test(getDelegate().getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
    }

    @Override
    @NonNull
    @SuppressWarnings("unchecked")
    public Mono<Void> writeWith(@Nonnull Publisher<? extends DataBuffer> body) {
        if (!canConsume()) {
            return super.writeWith(body);
        }
        if (body instanceof Flux) {
            // if body is not a flux. never got there.
            return super.writeWith(((Flux<? extends DataBuffer>) body).doOnNext(dataBuffer -> {
                DataBuffer slice = dataBuffer.slice(dataBuffer.readPosition(), dataBuffer.readableByteCount());
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                slice.read(bytes);
                cachedBody.add(bytes);
            }));
        }
        return super.writeWith(body);
    }

}
