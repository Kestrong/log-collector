package com.xjbg.log.collector.properties;

import com.xjbg.log.collector.LogCollectorConstant;
import com.xjbg.log.collector.enums.CollectorType;
import com.xjbg.log.collector.enums.NamingStrategy;
import com.xjbg.log.collector.enums.RejectPolicy;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * @author kesc
 * @since 2023-04-06 16:52
 */
@Getter
@Setter
public class LogCollectorProperties {
    public static final String PREFIX = "log.collector";
    private boolean enable = true;
    /**
     * the name of your application
     */
    private String application;
    /**
     * expression of spring advisor
     */
    private String pointcut;
    private String defaultCollectorType = CollectorType.COMMON.getType();
    /**
     * some properties in param that do not want to save,e.g. password,telephone, use ',' to separate multi
     */
    private String ignoreProperties;
    /**
     * enable schedule thread to clean up log
     */
    private boolean cleanUp = false;
    /**
     * max days to keep the log, larger than 0 will work
     */
    private int maxHistory = 30;
    private LogCollectorFilterProperties filter = new LogCollectorFilterProperties();
    private LogCollectorCustomProperties common = new LogCollectorCustomProperties();
    private DataBaseLogCollectorCustomProperties database = new DataBaseLogCollectorCustomProperties();
    private HttpLogCollectorCustomProperties http = new HttpLogCollectorCustomProperties();
    private EsLogCollectorCustomProperties es = new EsLogCollectorCustomProperties();
    private FeignLogCollectorCustomProperties feign = new FeignLogCollectorCustomProperties();

    @Getter
    @Setter
    public static class LogCollectorFilterProperties {
        private boolean enable = true;
        private int order = 0;
        private String tenantHeaderName = LogCollectorConstant.TENANT_HEADER_NAME;
        private String requestIdHeadName = LogCollectorConstant.REQUEST_ID_HEADER;
        private String userIdHeadName = LogCollectorConstant.USER_ID_HEADER;
        private String userTokenHeadName = LogCollectorConstant.AUTHORIZATION;
        private String userPropertyName = "userId";
        /**
         * customized userIdRetriever impl, can be a bean name or class name
         */
        private String userIdRetriever;
        private List<String> allowedHeaders = new ArrayList<>();
        private List<String> logPaths = new ArrayList<>();
        private List<String> excludePaths = new ArrayList<>();
        private List<String> consumeMediaType = Arrays.asList("application/json", "text/xml");
    }

    @Getter
    @Setter
    public static class LogCollectorJsonProperties {
        private String dateFormat;
        private NamingStrategy namingStrategy = NamingStrategy.CAMEL_CASE;
    }

    @Getter
    @Setter
    public static class LogCollectorChannelProperties {
        /**
         * customized channel class name
         */
        private String channelClass;
        /**
         * max channel record size, default to 10000
         */
        private Integer capacity;
        /**
         * max channel buffer size, default to 8M
         */
        private Integer byteCapacity;
        /**
         * bps to limit, larger than 0 is effective, default to 1M
         */
        private Integer byteSpeed;
        /**
         * tps to limit, larger than 0 is effective, default to 1000
         */
        private Integer recordSpeed;
        /**
         * time period to check speed, default to 3000
         */
        private Integer flowControlInterval;
        /**
         * threshold to trigger reject policy, suggest [0.5,0.9], default 0.8
         */
        private Float threshold;
    }

    @Getter
    @Setter
    public static class LogCollectorConnectionProperties {
        private int connectTimeout = 5000;
        private int socketTimeout = 30000;
        private int requestTimeout = 5000;
        private int maxConnect = 60;
        private int maxConnectPerRoute = 30;
        /**
         * ssl cert file path
         */
        private String certFile;
        /**
         * ignore https verify,just for dev and test,do not use in prod
         */
        private boolean ignoreHttps = false;
    }

    @Getter
    @Setter
    public static class LogCollectorCustomProperties {
        private boolean enable = false;
        private String fallbackCollector;
        private String nextCollector;
        private Integer poolSize;
        private Integer batchSize;
        private RejectPolicy rejectPolicy;
        /**
         * customized log transformer impl, can be a bean name or class name
         */
        private String logTransformerImpl;
        private LogCollectorChannelProperties channel = new LogCollectorChannelProperties();
        private Map<String, String> properties = new HashMap<>();
    }

    @Getter
    @Setter
    public static class DataBaseLogCollectorCustomProperties extends LogCollectorCustomProperties {
        private String tableName;
        /**
         * token for wrap tableName or field, e.g. `log_info`
         */
        private String wrapper;
    }

    @Getter
    @Setter
    public static class HttpLogCollectorCustomProperties extends LogCollectorCustomProperties {
        private String url;
        private String method;
        private String charset;
        /**
         * customized http header creator impl, can be a bean name or class name
         */
        private String tokenHeaderCreator;
        private LogCollectorJsonProperties json = new LogCollectorJsonProperties();
        private LogCollectorConnectionProperties connection = new LogCollectorConnectionProperties();
    }

    @Getter
    @Setter
    public static class EsLogCollectorCustomProperties extends LogCollectorCustomProperties {
        private String index;
        /**
         * elastic search version, optional 7 or 8
         */
        private String version = "7";
        /**
         * elastic search server host, e.g. 127.0.0.1:9200
         */
        private String hosts;
        private String schema = "http";
        private String clusterName = "es-cluster";
        private String username;
        private String password;
        private LogCollectorJsonProperties json = new LogCollectorJsonProperties();
        private LogCollectorConnectionProperties connection = new LogCollectorConnectionProperties();
    }

    @Getter
    @Setter
    public static class FeignLogCollectorCustomProperties extends LogCollectorCustomProperties {
        private String name;
        private String method;
        private String path;
        private String url = "";
    }

}
