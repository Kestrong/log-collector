package com.xjbg.log.collector.api.impl;

import com.xjbg.log.collector.model.LogInfo;
import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;

import java.util.Date;

/**
 * @author kesc
 * @since 2023-04-11 10:12
 */
@Getter
@Setter
public class Es7LogCollector extends AbstractEsLogCollector {
    private RestHighLevelClient highLevelClient;

    public Es7LogCollector(RestHighLevelClient highLevelClient) {
        this.highLevelClient = highLevelClient;
    }

    @Override
    @SuppressWarnings("all")
    protected void doLog(LogInfo logInfo) throws Exception {
        IndexRequest indexRequest = new IndexRequest();
        indexRequest.id(logInfo.getLogId()).index(getIndex()).source(toJsonString(logInfo), XContentType.JSON);
        getHighLevelClient().index(indexRequest, RequestOptions.DEFAULT);
    }

    @Override
    public void cleanLog(Date before) throws Exception {
        log.info("clean up log before:{}", before);
        DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest();
        QueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .filter(QueryBuilders.rangeQuery(getTimeFieldName()).lte(before));
        deleteByQueryRequest.indices(getIndex());
        deleteByQueryRequest.setQuery(queryBuilder);
        getHighLevelClient().deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
    }
}
