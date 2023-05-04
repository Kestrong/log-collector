package com.xjbg.log.collector.token;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;

/**
 * @author kesc
 * @since 2023-04-14 20:06
 */
public class BasicAuthHttpTokenCreator extends DefaultHttpTokenCreator {
    private String token;

    @Override
    public Map<String, String> tokenHeader() {
        return Collections.singletonMap("Authorization", "Basic " + token);
    }

    @Override
    public void afterProperties(Map<String, String> properties) {
        if (properties != null && !properties.isEmpty()) {
            String username = properties.get("username");
            String password = properties.get("password");
            String charset = properties.getOrDefault("charset", StandardCharsets.UTF_8.name());
            try {
                token = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(charset));
            } catch (UnsupportedEncodingException e) {
                token = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
            }
        }
    }
}
