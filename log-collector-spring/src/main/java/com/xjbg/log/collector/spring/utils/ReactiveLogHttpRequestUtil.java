package com.xjbg.log.collector.spring.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;

import java.net.InetAddress;

/**
 * @author kesc
 * @since 2023-07-24 10:33
 */
public class ReactiveLogHttpRequestUtil {

    public static String getRequestIp(ServerHttpRequest request) {
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

    public static String getUserAgent(ServerHttpRequest request) {
        try {
            if (request == null) {
                return null;
            }
            return request.getHeaders().getFirst("User-Agent");
        } catch (Exception e) {
            return null;
        }
    }

    public static String getRequestURL(ServerHttpRequest request) {
        try {
            if (request == null) {
                return null;
            }
            return request.getPath().pathWithinApplication().value();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getRequestMethod(ServerHttpRequest request) {
        try {
            if (request == null) {
                return null;
            }
            return request.getMethodValue();
        } catch (Exception e) {
            return null;
        }
    }
}
