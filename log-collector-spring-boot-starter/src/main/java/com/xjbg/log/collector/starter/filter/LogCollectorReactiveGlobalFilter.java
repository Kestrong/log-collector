package com.xjbg.log.collector.starter.filter;

import com.xjbg.log.collector.LogCollectors;
import com.xjbg.log.collector.enums.LogState;
import com.xjbg.log.collector.model.LogInfo;
import com.xjbg.log.collector.properties.LogCollectorProperties;
import com.xjbg.log.collector.request.AbstractLogCollectorGlobalFilter;
import com.xjbg.log.collector.utils.ExceptionUtil;
import com.xjbg.log.collector.utils.JsonLogUtil;
import com.xjbg.log.collector.utils.RequestIdHolder;
import com.xjbg.log.collector.utils.UserEnv;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.net.InetAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

/**
 * @author kesc
 * @since 2023-06-08 10:47
 */
public class LogCollectorReactiveGlobalFilter extends AbstractLogCollectorGlobalFilter implements WebFilter, Ordered {

    public LogCollectorReactiveGlobalFilter(LogCollectorProperties properties) {
        super(properties);
    }

    private String getRequestIp(ServerHttpRequest request) {
        try {
            if (request == null) {
                return null;
            }
            String ip = request.getHeaders().getFirst("X-Forwarded-For");
            if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeaders().getFirst("Proxy-Client-IP");
            }
            if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeaders().getFirst("WL-Proxy-Client-IP");
            }
            if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                if (request.getRemoteAddress() != null) {
                    ip = request.getRemoteAddress().getAddress().getHostAddress();
                    if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
                        try {
                            InetAddress inet = InetAddress.getLocalHost();
                            ip = inet.getHostAddress();
                        } catch (Exception e) {
                            //ignore
                        }
                    }
                }
            }
            if (ip != null && ip.length() > 15) {
                if (ip.indexOf(",") > 0) {
                    ip = ip.substring(0, ip.indexOf(","));
                }
            }
            return ip;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    protected void doLogPaths(LogInfo.LogInfoBuilder builder, MutableServerHttpRequestDecorator request) {
        try {
            boolean match = match(request.getPath().pathWithinApplication().value());
            if (!match) {
                return;
            }
            Map<String, Object> payload = new HashMap<>();
            Map<String, String> params = new HashMap<>();
            MultiValueMap<String, String> queryParams = request.getQueryParams();
            for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
                String paramName = entry.getKey();
                List<String> parameterValues = entry.getValue();
                if (parameterValues != null && !parameterValues.isEmpty()) {
                    params.put(paramName, Arrays.toString(parameterValues.toArray()));
                }
            }
            payload.put("params", params);
            byte[] requestBody = request.getRequestBody();
            payload.put("body", requestBody == null || requestBody.length == 0 ? null : new String(requestBody));
            try {
                LogCollectors.defaultCollector().logAsync(builder
                        .responseTime(new Date())
                        .userAgent(request.getHeaders().getFirst("User-Agent"))
                        .requestMethod(request.getMethodValue())
                        .requestIp(getRequestIp(request))
                        .params(JsonLogUtil.toJson(payload)).build());
            } catch (Exception e) {
                //ignore
            }
        } catch (Exception e) {
            //ignore
        }
    }

    @Override
    @Nonnull
    public Mono<Void> filter(@Nonnull ServerWebExchange exchange, @Nonnull WebFilterChain chain) {
        if (!properties.getFilter().isEnable()) {
            return chain.filter(exchange);
        }
        LogInfo.LogInfoBuilder builder = LogInfo.builder().requestTime(new Date());
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String requestIdName = properties.getFilter().getRequestIdHeadName();
        String userIdHeadName = properties.getFilter().getUserIdHeadName();
        ServerHttpRequest httpRequest = exchange.getRequest().mutate().build();
        ServerHttpResponse httpResponse = exchange.getResponse();
        try {
            //do requestId
            String requestId = httpRequest.getHeaders().getFirst(requestIdName);
            if (StringUtils.isBlank(requestId)) {
                requestId = httpRequest.getQueryParams().getFirst(requestIdName);
                if (StringUtils.isBlank(requestId)) {
                    requestId = UUID.randomUUID().toString();
                }
                httpRequest = httpRequest.mutate().header(requestIdName, requestId).build();
            }
            if (!httpResponse.getHeaders().containsKey(requestIdName)) {
                httpResponse.getHeaders().add(requestIdName, requestId);
            }
            builder.requestId(requestId);
            //do userId
            String userTokenName = properties.getFilter().getUserTokenHeadName();
            String token = httpRequest.getHeaders().getFirst(userTokenName);
            if (StringUtils.isBlank(token)) {
                token = httpRequest.getQueryParams().getFirst(userTokenName);
            }
            String userId = userIdRetriever != null ? userIdRetriever.getUserId(token) : null;
            if (StringUtils.isNotBlank(userId)) {
                httpRequest = httpRequest.mutate().header(userIdHeadName, userId).build();
                if (!httpResponse.getHeaders().containsKey(userIdHeadName)) {
                    httpResponse.getHeaders().add(userIdHeadName, userId);
                }
                builder.userId(userId);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        stopWatch.stop();
        log.debug("log collector global filter cost microseconds: {}", stopWatch.getTime(TimeUnit.MICROSECONDS));
        //do log paths
        Predicate<String> canConsume = this::canConsume;
        MutableServerHttpRequestDecorator httpRequestDecorator = new MutableServerHttpRequestDecorator(httpRequest, canConsume);
        MutableServerHttpResponseDecorator httpResponseDecorator = new MutableServerHttpResponseDecorator(httpResponse, canConsume);
        return chain.filter(exchange.mutate().request(httpRequestDecorator).response(httpResponseDecorator).build())
                .doOnError(cx -> builder.state(LogState.FAIL.name()).response(ExceptionUtil.getTraceInfo(cx.getCause())))
                .doOnSuccess(cx -> {
                    byte[] content = httpResponseDecorator.getContent();
                    if (content != null && content.length > 0) {
                        builder.response(new String(content));
                    }
                    HttpStatus rawStatusCode = httpResponseDecorator.getStatusCode();
                    if (rawStatusCode != null && rawStatusCode.value() < 300) {
                        builder.state(LogState.SUCCESS.name());
                    } else {
                        builder.state(LogState.FAIL.name());
                    }
                }).doFinally(x -> {
                    stopWatch.reset();
                    stopWatch.start();
                    URI uri = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
                    if (uri != null) {
                        builder.requestUrl(uri.toString());
                    } else {
                        Set<URI> uris = exchange.getAttributeOrDefault(GATEWAY_ORIGINAL_REQUEST_URL_ATTR, Collections.emptySet());
                        String originalUri = (uris.isEmpty()) ? "Unknown" : uris.iterator().next().toString();
                        builder.requestUrl(originalUri);
                    }
                    doLogPaths(builder, httpRequestDecorator);
                    stopWatch.stop();
                    log.debug("log path cost microseconds: {}", stopWatch.getTime(TimeUnit.MICROSECONDS));
                });
    }

    @Override
    public int getOrder() {
        return properties.getFilter().getOrder();
    }
}
