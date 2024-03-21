package com.xjbg.log.collector.utils;

import org.apache.commons.lang3.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;

/**
 * @author kesc
 * @since 2023-04-12 9:24
 */
public class LogHttpRequestUtil {

    public static String getRequestIp(HttpServletRequest request) {
        try {
            if (request == null) {
                return null;
            }
            String ip = request.getHeader("X-Forwarded-For");
            if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
                if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
                    try {
                        InetAddress inet = InetAddress.getLocalHost();
                        ip = inet.getHostAddress();
                    } catch (Exception e) {
                        //ignore
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

    public static String getUserAgent(HttpServletRequest request) {
        try {
            if (request == null) {
                return null;
            }
            return request.getHeader("User-Agent");
        } catch (Exception e) {
            return null;
        }
    }

    public static String getRequestURL(HttpServletRequest request) {
        try {
            if (request == null) {
                return null;
            }
            return request.getRequestURL().toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getRequestMethod(HttpServletRequest request) {
        try {
            if (request == null) {
                return null;
            }
            return request.getMethod();
        } catch (Exception e) {
            return null;
        }
    }
}
