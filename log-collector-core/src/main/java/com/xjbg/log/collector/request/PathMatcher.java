package com.xjbg.log.collector.request;

/**
 * @author kesc
 * @since 2023-04-20 14:07
 */
public interface PathMatcher {
    /**
     * Match the given {@code path} against the given {@code pattern},
     * according to this PathMatcher's matching strategy.
     *
     * @param pattern the pattern to match against
     * @param path    the path to test
     * @return {@code true} if the supplied {@code path} matched,
     * {@code false} if it didn't
     */
    boolean match(String pattern, String path);

    class False implements PathMatcher {
        private static final False INSTANCE = new False();

        private False() {
        }

        public static False getInstance() {
            return INSTANCE;
        }

        @Override
        public boolean match(String pattern, String path) {
            return false;
        }

    }
}
