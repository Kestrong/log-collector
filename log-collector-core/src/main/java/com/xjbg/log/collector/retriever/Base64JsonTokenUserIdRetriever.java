package com.xjbg.log.collector.retriever;

import com.xjbg.log.collector.utils.JsonLogUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * @author kesc
 * @since 2023-04-14 21:19
 */
@Slf4j
@Getter
@Setter
public class Base64JsonTokenUserIdRetriever extends UserIdRetriever {
    private String userPropertyName = "userId";
    private String charset = StandardCharsets.UTF_8.name();

    @Override
    public void setUserPropertyName(String userPropertyName) {
        this.userPropertyName = userPropertyName;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public String getUserId(Object token) {
        if (token != null && !"".equals(token)) {
            String[] bs = String.valueOf(token).split("\\.");
            String base64EncodedBody = bs.length > 1 ? bs[1] : bs[0];
            try {
                String body = new String(Base64.getUrlDecoder().decode(base64EncodedBody.getBytes()), charset);
                Map value = JsonLogUtil.getDefaultObjectMapper().readerFor(Map.class).readValue(body);
                return String.valueOf(value.get(userPropertyName));
            } catch (Exception e) {
                log.error(e.getMessage());
                return null;
            }
        }
        return null;
    }

}
