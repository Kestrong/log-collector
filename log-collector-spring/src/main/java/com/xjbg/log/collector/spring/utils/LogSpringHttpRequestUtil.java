package com.xjbg.log.collector.spring.utils;

import com.xjbg.log.collector.utils.LogHttpRequestUtil;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author kesc
 * @since 2023-04-12 9:24
 */
public class LogSpringHttpRequestUtil extends LogHttpRequestUtil {

    public static HttpServletRequest getRequest() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getRequestIp() {
        try {
            return getRequestIp(getRequest());
        } catch (Exception e) {
            return null;
        }
    }

    public static String getUserAgent() {
        try {
            return getUserAgent(getRequest());
        } catch (Exception e) {
            return null;
        }
    }

    public static String getRequestURL() {
        try {
            return getRequestURL(getRequest());
        } catch (Exception e) {
            return null;
        }
    }

    public static String getRequestMethod() {
        try {
            return getRequestMethod(getRequest());
        } catch (Exception e) {
            return null;
        }
    }
}
