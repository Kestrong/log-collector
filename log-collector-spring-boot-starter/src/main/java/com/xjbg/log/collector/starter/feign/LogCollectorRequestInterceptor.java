package com.xjbg.log.collector.starter.feign;

import com.xjbg.log.collector.properties.LogCollectorProperties;
import com.xjbg.log.collector.spring.utils.LogSpringHttpRequestUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * @author kesc
 * @since 2023-04-17 10:06
 */
@SuppressWarnings("unused")
public class LogCollectorRequestInterceptor implements RequestInterceptor {
    private final LogCollectorProperties properties;

    public LogCollectorRequestInterceptor(LogCollectorProperties properties) {
        this.properties = properties;
    }

    @Override
    public void apply(RequestTemplate requestTemplate) {
        if (!properties.getFilter().isEnable()) {
            return;
        }
        HttpServletRequest request = LogSpringHttpRequestUtil.getRequest();
        if (request == null) {
            return;
        }
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames == null) {
            return;
        }
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            String value = request.getHeader(name);
            String requestIdHeadName = properties.getFilter().getRequestIdHeadName();
            if (name.equals(requestIdHeadName)) {
                requestTemplate.header(name, value);
            } else if (name.equals(properties.getFilter().getUserTokenHeadName())) {
                requestTemplate.header(name, value);
            } else if (!CollectionUtils.isEmpty(properties.getFilter().getAllowedHeaders()) && properties.getFilter().getAllowedHeaders().contains(name)) {
                requestTemplate.header(name, value);
            }
        }
    }

}
