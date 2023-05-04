package com.xjbg.log.collector.starter.filter;

import com.xjbg.log.collector.utils.ByteUtil;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;
import reactor.util.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author kesc
 * @since 2023-04-23 14:07
 */
public class MutableServerHttpRequestDecorator extends ServerHttpRequestDecorator {
    private final Predicate<String> canConsume;
    private final List<byte[]> cachedBody;

    public MutableServerHttpRequestDecorator(ServerHttpRequest delegate, Predicate<String> canConsume) {
        super(delegate);
        this.canConsume = canConsume;
        cachedBody = new ArrayList<>();
    }

    public byte[] getRequestBody() {
        return cachedBody.size() > 0 ? ByteUtil.mergeBytes(cachedBody) : null;
    }

    private boolean canConsume() {
        return canConsume != null && canConsume.test(getDelegate().getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
    }

    @Override
    @NonNull
    public Flux<DataBuffer> getBody() {
        if (!canConsume()) {
            return super.getBody();
        }
        return super.getBody().doOnNext(dataBuffer -> {
            DataBuffer slice = dataBuffer.slice(dataBuffer.readPosition(), dataBuffer.readableByteCount());
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            slice.read(bytes);
            cachedBody.add(bytes);
        });
    }

}
