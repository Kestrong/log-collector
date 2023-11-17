package com.xjbg.log.collector.token;

import java.util.Collections;
import java.util.Map;

/**
 * @author kesc
 * @since 2023-04-07 11:04
 */
public class DefaultHttpTokenCreator implements IHttpTokenCreator {

    @Override
    public Map<String, String> tokenHeader() {
        return Collections.emptyMap();
    }

    @Override
    public String parseUrl(String url, Object param) {
        return url;
    }

    @Override
    public void afterProperties(Map<String, String> properties) {

    }
}
