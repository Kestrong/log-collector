package com.xjbg.log.collector.model;

import com.xjbg.log.collector.utils.JsonLogUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.StringJoiner;

/**
 * @author kesc
 * @since 2023-03-30 9:47
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogInfo implements Serializable {
    private String logId;
    private String userId;
    private String application;
    private String businessNo;
    private String module;
    private String state;
    private String action;
    private String type;
    private String handleMethod;
    private String userAgent;
    private String requestId;
    private String requestIp;
    private String requestUrl;
    private String requestMethod;
    private Date requestTime;
    private Date createTime;
    private Date responseTime;
    private Object params;
    private Object response;

    private String toJson(Object o) {
        return JsonLogUtil.toJson(o);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LogInfo.class.getSimpleName() + "[", "]")
                .add("logId='" + logId + "'")
                .add("userId='" + userId + "'")
                .add("application='" + application + "'")
                .add("businessNo='" + businessNo + "'")
                .add("module='" + module + "'")
                .add("state='" + state + "'")
                .add("action='" + action + "'")
                .add("type='" + type + "'")
                .add("handleMethod='" + handleMethod + "'")
                .add("userAgent='" + userAgent + "'")
                .add("requestId='" + requestId + "'")
                .add("requestIp='" + requestIp + "'")
                .add("requestUrl='" + requestUrl + "'")
                .add("requestMethod='" + requestMethod + "'")
                .add("requestTime=" + requestTime)
                .add("createTime=" + createTime)
                .add("responseTime=" + responseTime)
                .add("params=" + toJson(params))
                .add("response=" + toJson(response))
                .toString();
    }
}
