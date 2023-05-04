package com.xjbg.log.collector.retriever;

/**
 * @author kesc
 * @since 2023-04-17 9:20
 */
public class SimpleUserIdRetriever extends UserIdRetriever {

    @Override
    public String getUserId(Object token) {
        if (token == null) {
            return null;
        }
        return String.valueOf(token);
    }

}
