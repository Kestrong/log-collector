package com.xjbg.log.collector.spring.utils;

import com.xjbg.log.collector.request.RexPathMatcher;
import org.springframework.util.AntPathMatcher;

/**
 * @author kesc
 * @since 2023-04-20 16:18
 */
public class CompositeRexAntPathMatcher extends RexPathMatcher {
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public boolean match(String pattern, String path) {
        String[] parts = pattern.split(":");
        if (parts.length > 1) {
            if ("rex".equalsIgnoreCase(parts[0])) {
                return super.match(parts[1], path);
            }
            return antPathMatcher.match(parts[1], path);
        }
        return antPathMatcher.match(pattern, path);
    }

}
