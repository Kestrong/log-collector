package com.xjbg.log.collector.spring.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * @author kesc
 * @since 2023-07-25 8:42
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactiveRequestContext {
    private ServerHttpRequest request;
    private ServerHttpResponse response;
}
