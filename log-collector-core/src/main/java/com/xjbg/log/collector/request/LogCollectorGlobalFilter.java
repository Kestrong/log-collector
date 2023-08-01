package com.xjbg.log.collector.request;

import com.xjbg.log.collector.LogCollectors;
import com.xjbg.log.collector.enums.LogState;
import com.xjbg.log.collector.model.LogContext;
import com.xjbg.log.collector.model.LogInfo;
import com.xjbg.log.collector.properties.LogCollectorProperties;
import com.xjbg.log.collector.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.MDC;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * @author kesc
 * @since 2023-04-14 12:31
 */
public class LogCollectorGlobalFilter extends AbstractLogCollectorGlobalFilter implements Filter {

    public LogCollectorGlobalFilter(LogCollectorProperties properties) {
        super(properties);
    }

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("init requestId filter, requestIdName:{}, userTokenName:{}.", properties.getFilter().getRequestIdHeadName(), properties.getFilter().getUserTokenHeadName());
    }

    @SuppressWarnings("unchecked")
    protected void doLogPaths(LogInfo.LogInfoBuilder builder, MutableHttpServletRequestWrapper request, MutableHttpServletResponseWrapper response, String exception) {
        try {
            boolean match = match(request.getServletPath());
            if (!match) {
                return;
            }
            Map<String, Object> payload = new HashMap<>();
            Map<String, String> params = new HashMap<>();
            Enumeration<String> parameterNames = request.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String paramName = parameterNames.nextElement();
                String[] parameterValues = request.getParameterValues(paramName);
                if (parameterValues != null) {
                    params.put(paramName, String.join(",", parameterValues));
                }
            }
            payload.put("params", params);
            byte[] requestBody = request.getRequestBody();
            payload.put("body", requestBody == null || requestBody.length == 0 ? null : new String(requestBody));
            try {
                if (exception != null) {
                    builder.response(exception).state(LogState.FAIL.name());
                } else {
                    byte[] content = response.getContent();
                    builder.response(content == null || content.length == 0 ? null : new String(content)).state(response.getStatus() < 300 ? LogState.SUCCESS.name() : LogState.FAIL.name());
                }
                LogCollectors.defaultCollector().logAsync(builder.responseTime(new Date()).userAgent(LogHttpRequestUtil.getUserAgent(request)).requestUrl(LogHttpRequestUtil.getRequestURL(request)).requestMethod(LogHttpRequestUtil.getRequestMethod(request)).requestIp(LogHttpRequestUtil.getRequestIp(request)).params(JsonLogUtil.toJson(payload)).build());
            } catch (Exception e) {
                //ignore
            }
        } catch (Exception e) {
            //ignore
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!properties.getFilter().isEnable()) {
            chain.doFilter(request, response);
            return;
        }
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }
        LogInfo.LogInfoBuilder builder = LogInfo.builder().requestTime(new Date());
        LogContext.LogContextBuilder logContextBuilder = LogContext.builder();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String requestIdName = properties.getFilter().getRequestIdHeadName();
        try {
            Predicate<String> canConsume = this::canConsume;

            MutableHttpServletRequestWrapper httpServletRequest = new MutableHttpServletRequestWrapper((HttpServletRequest) request, canConsume);
            MutableHttpServletResponseWrapper httpServletResponse = new MutableHttpServletResponseWrapper((HttpServletResponse) response, canConsume);
            try {
                //do requestId
                String header = httpServletRequest.getHeader(requestIdName);
                if (StringUtils.isBlank(header)) {
                    header = httpServletRequest.getParameter(requestIdName);
                    if (StringUtils.isBlank(header)) {
                        Object attribute = httpServletRequest.getAttribute(requestIdName);
                        header = attribute == null ? null : String.valueOf(attribute);
                    }
                    if (StringUtils.isBlank(header)) {
                        header = UUID.randomUUID().toString();
                    }
                    httpServletRequest.addHeader(requestIdName, header);
                }
                logContextBuilder.requestId(header);
                if (!httpServletResponse.containsHeader(requestIdName)) {
                    httpServletResponse.addHeader(requestIdName, header);
                }
                MDC.put(requestIdName, header);
                RequestIdHolder.setRequestId(header);
                //do userId
                String userTokenName = properties.getFilter().getUserTokenHeadName();
                String token = httpServletRequest.getHeader(userTokenName);
                if (StringUtils.isBlank(token)) {
                    token = httpServletRequest.getParameter(userTokenName);
                    if (StringUtils.isBlank(token)) {
                        Object attribute = httpServletRequest.getAttribute(userTokenName);
                        token = attribute == null ? null : String.valueOf(attribute);
                    }
                }
                String userId = userIdRetriever != null ? userIdRetriever.getUserId(token) : null;
                if (StringUtils.isNotBlank(userId)) {
                    String userIdHeadName = properties.getFilter().getUserIdHeadName();
                    httpServletRequest.addHeader(userIdHeadName, userId);
                    UserEnv.setUser(userId);
                    logContextBuilder.userId(userId);
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
            LogContextHolder.setContext(logContextBuilder.build());
            stopWatch.stop();
            log.debug("log collector global filter cost microseconds: {}", stopWatch.getTime(TimeUnit.MICROSECONDS));
            //do log paths
            String exception = null;
            try {
                chain.doFilter(httpServletRequest, httpServletResponse);
            } catch (Throwable t) {
                exception = ExceptionUtil.getTraceInfo(t);
                throw t;
            } finally {
                stopWatch.reset();
                stopWatch.start();
                doLogPaths(builder, httpServletRequest, httpServletResponse, exception);
                stopWatch.stop();
                log.debug("log path cost microseconds: {}", stopWatch.getTime(TimeUnit.MICROSECONDS));
            }
        } finally {
            MDC.remove(requestIdName);
            RequestIdHolder.remove();
            UserEnv.remove();
            LogContextHolder.remove();
        }
    }

    @Override
    public void destroy() {
        log.info("destroy requestId filter.");
    }

}
