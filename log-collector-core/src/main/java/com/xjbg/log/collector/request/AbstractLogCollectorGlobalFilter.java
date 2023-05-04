package com.xjbg.log.collector.request;

import com.xjbg.log.collector.properties.LogCollectorProperties;
import com.xjbg.log.collector.retriever.UserIdRetriever;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author kesc
 * @since 2023-04-23 11:24
 */
@Getter
@Setter
public abstract class AbstractLogCollectorGlobalFilter {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected UserIdRetriever userIdRetriever = UserIdRetriever.Null.INSTANCE;
    protected final LogCollectorProperties properties;
    protected PathMatcher pathMatcher = PathMatcher.False.getInstance();

    public AbstractLogCollectorGlobalFilter(LogCollectorProperties properties) {
        this.properties = properties;
    }

    protected boolean match(String servletPath) {
        List<String> logPaths = properties.getFilter().getLogPaths();
        if (logPaths == null || logPaths.isEmpty()) {
            return false;
        }
        PathMatcher pathMatcher = getPathMatcher();
        List<String> excludePaths = properties.getFilter().getExcludePaths();
        if (excludePaths != null && !excludePaths.isEmpty()) {
            for (String excludePath : excludePaths) {
                if (pathMatcher.match(excludePath, servletPath)) {
                    return false;
                }
            }
        }
        boolean match = false;
        for (String logPath : logPaths) {
            match = pathMatcher.match(logPath, servletPath);
            if (match) {
                log.debug("match log path pattern:{}, request path:{}", logPath, servletPath);
                break;
            }
        }
        return match;
    }

    protected boolean canConsume(String contentType) {
        if (StringUtils.isBlank(contentType)) {
            return false;
        }
        List<String> consumeMediaTypes = getProperties().getFilter().getConsumeMediaType();
        if (consumeMediaTypes == null || consumeMediaTypes.isEmpty()) {
            return true;
        }
        return consumeMediaTypes.stream().anyMatch(x -> StringUtils.startsWithIgnoreCase(contentType, x));
    }

}
