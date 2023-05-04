package com.xjbg.log.collector.retriever;

/**
 * @author kesc
 * @since 2023-04-14 17:42
 */
public abstract class UserIdRetriever {

    public abstract String getUserId(Object token);

    public void setUserPropertyName(String userPropertyName) {

    }

    public static class Null extends UserIdRetriever {
        public static final UserIdRetriever INSTANCE = new Null();

        private Null() {
        }

        @Override
        public String getUserId(Object token) {
            return null;
        }
    }
}
