package com.xjbg.log.collector.request;

import java.util.regex.Pattern;

/**
 * @author kesc
 * @since 2023-04-20 14:08
 */
public class RexPathMatcher implements PathMatcher {

    @Override
    public boolean match(String pattern, String path) {
        return Pattern.matches(pattern, path);
    }

}
