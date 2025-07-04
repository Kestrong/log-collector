package com.xjbg.log.collector.token;

import com.xjbg.log.collector.model.LogInfo;
import com.xjbg.log.collector.utils.JsonLogUtil;

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
        if (!url.contains("{")) {
            return url;
        }
        Map<?, ?> paramMap;
        if (param instanceof LogInfo) {
            paramMap = JsonLogUtil.fromJson(JsonLogUtil.toJson(param), Map.class);
        } else if (param instanceof Map) {
            paramMap = (Map<?, ?>) param;
        } else {
            return url;
        }

        for (Map.Entry<?, ?> entry : paramMap.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (url.contains(placeholder)) {
                url = url.replace(placeholder, entry.getValue().toString());
            }
        }
        return url;
    }

    @Override
    public void afterProperties(Map<String, String> properties) {

    }
}
