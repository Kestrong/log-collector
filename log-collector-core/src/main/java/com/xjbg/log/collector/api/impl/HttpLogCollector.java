package com.xjbg.log.collector.api.impl;

import com.xjbg.log.collector.enums.CollectorType;
import com.xjbg.log.collector.model.LogInfo;
import com.xjbg.log.collector.token.DefaultHttpTokenCreator;
import com.xjbg.log.collector.token.IHttpTokenCreator;
import lombok.Getter;
import lombok.Setter;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

/**
 * @author kesc
 * @since 2023-03-30 12:13
 */
@Getter
@Setter
public class HttpLogCollector extends AbstractJsonLogCollector<LogInfo, Object> {
    private String url;
    private String method = HttpPost.METHOD_NAME;
    private String charset = StandardCharsets.UTF_8.name();
    private IHttpTokenCreator tokenCreator;
    private HttpClient httpClient;
    private static final ResponseHandler<String> BASIC_RESPONSE_HANDLER = new BasicResponseHandler();

    public HttpLogCollector(String url, HttpClient httpClient) {
        super();
        this.url = url;
        this.tokenCreator = new DefaultHttpTokenCreator();
        this.httpClient = httpClient;
    }

    @Override
    public String type() {
        return CollectorType.HTTP.getType();
    }

    protected String getUrl() {
        IHttpTokenCreator httpTokenCreator = getTokenCreator();
        return httpTokenCreator != null ? httpTokenCreator.authUrl(url) : url;
    }

    protected Map<String, String> tokenHeaders() {
        IHttpTokenCreator httpTokenCreator = getTokenCreator();
        return httpTokenCreator == null ? Collections.emptyMap() : httpTokenCreator.tokenHeader();
    }

    protected void execute(String url, Object requestBody) throws Exception {
        HttpClient httpClient = getHttpClient();
        String method = getMethod();
        HttpEntityEnclosingRequestBase requestBase = new HttpEntityEnclosingRequestBase() {
            @Override
            public String getMethod() {
                return method;
            }
        };
        requestBase.setURI(URI.create(url));
        for (Map.Entry<String, String> entry : tokenHeaders().entrySet()) {
            requestBase.addHeader(entry.getKey(), entry.getValue());
        }
        String requestBodyJsonString = null;
        if (requestBody != null) {
            requestBodyJsonString = toJsonString(requestBody);
            requestBase.setEntity(new StringEntity(requestBodyJsonString, ContentType.APPLICATION_JSON.withCharset(getCharset())));
        }
        HttpResponse response = httpClient.execute(requestBase);
        log.debug("send log to {}, result:{}, body:{}", url, response.getStatusLine().getStatusCode(), requestBodyJsonString);
        validResult(response);
    }

    protected void validResult(HttpResponse response) throws Exception {
        BASIC_RESPONSE_HANDLER.handleResponse(response);
    }

    @Override
    protected void doLog(Object logInfo) throws Exception {
        execute(getUrl(), logInfo);
    }

}
