package com.xjbg.log.collector.spring.filter;

import com.xjbg.log.collector.LogCollectors;
import com.xjbg.log.collector.enums.LogState;
import com.xjbg.log.collector.model.LogContext;
import com.xjbg.log.collector.model.LogInfo;
import com.xjbg.log.collector.properties.LogCollectorProperties;
import com.xjbg.log.collector.request.AbstractLogCollectorGlobalFilter;
import com.xjbg.log.collector.spring.model.ReactiveRequestContext;
import com.xjbg.log.collector.spring.utils.ReactiveLogHttpRequestUtil;
import com.xjbg.log.collector.spring.utils.ReactiveRequestContextHolder;
import com.xjbg.log.collector.utils.ExceptionUtil;
import com.xjbg.log.collector.utils.JsonLogUtil;
import com.xjbg.log.collector.utils.ReactiveLogContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * @author kesc
 * @since 2023-06-08 10:47
 */
public class LogCollectorReactiveGlobalFilter extends AbstractLogCollectorGlobalFilter implements WebFilter, Ordered {

    public LogCollectorReactiveGlobalFilter(LogCollectorProperties properties) {
        super(properties);
    }

    @SuppressWarnings("unchecked")
    protected void doLogPaths(LogInfo.LogInfoBuilder builder, MutableServerHttpRequestDecorator request) {
        try {
            String requestURL = ReactiveLogHttpRequestUtil.getRequestURL(request);
            boolean match = match(requestURL);
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
                        .requestUrl(requestURL)
                        .userAgent(ReactiveLogHttpRequestUtil.getUserAgent(request))
                        .requestMethod(ReactiveLogHttpRequestUtil.getRequestMethod(request))
                        .requestIp(ReactiveLogHttpRequestUtil.getRequestIp(request))
                        .params(JsonLogUtil.toJson(payload)).build());
            } catch (Exception e) {
                //ignore
            }
        } catch (Exception e) {
            //ignore
        }
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        if (!properties.getFilter().isEnable()) {
            return chain.filter(exchange);
        }
        LogInfo.LogInfoBuilder builder = LogInfo.builder().requestTime(new Date());
        LogContext.LogContextBuilder logContextBuilder = LogContext.builder();
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
            logContextBuilder.requestId(requestId);
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
                logContextBuilder.userId(userId);
                builder.userId(userId);
            }
            logContextBuilder.tenantId(httpRequest.getHeaders().getFirst(properties.getFilter().getTenantHeaderName()));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        stopWatch.stop();
        log.debug("log collector global filter cost microseconds: {}", stopWatch.getTime(TimeUnit.MICROSECONDS));
        //do log paths
        Predicate<String> canConsume = this::canConsume;
        MutableServerHttpRequestDecorator httpRequestDecorator = new MutableServerHttpRequestDecorator(httpRequest, canConsume);
        MutableServerHttpResponseDecorator httpResponseDecorator = new MutableServerHttpResponseDecorator(httpResponse, canConsume);
        Mono<Void> mono = ReactiveLogContextHolder.setContext(chain.filter(exchange.mutate().request(httpRequestDecorator).response(httpResponseDecorator).build())
                .doOnError(cx -> builder.state(LogState.FAIL.name()).response(ExceptionUtil.getTraceInfo(cx.getCause())))
                .doOnSuccess(cx -> {
                    byte[] content = httpResponseDecorator.getContent();
                    if (content != null && content.length > 0) {
                        builder.response(new String(content));
                    }
                    HttpStatusCode rawStatusCode = httpResponseDecorator.getStatusCode();
                    if (rawStatusCode != null && rawStatusCode.value() < 300) {
                        builder.state(LogState.SUCCESS.name());
                    } else {
                        builder.state(LogState.FAIL.name());
                    }
                }).doFinally(x -> {
                    stopWatch.reset();
                    stopWatch.start();
                    doLogPaths(builder, httpRequestDecorator);
                    stopWatch.stop();
                    log.debug("log path cost microseconds: {}", stopWatch.getTime(TimeUnit.MICROSECONDS));
                }), logContextBuilder.build());
        return ReactiveRequestContextHolder.setRequestContext(mono, ReactiveRequestContext.builder().request(httpRequestDecorator).response(httpResponseDecorator).build());
    }

    @Override
    public int getOrder() {
        return properties.getFilter().getOrder();
    }
}
