package com.xjbg.log.collector.token;

import java.util.Map;

/**
 * @author kesc
 * @since 2023-04-07 11:02
 */
public interface IHttpTokenCreator {

    Map<String, String> tokenHeader();

    String authUrl(String url);

    void afterProperties(Map<String, String> properties);
}
