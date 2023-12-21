package com.xjbg.log.collector.api.impl;

import com.xjbg.log.collector.LogCollectorConstant;
import com.xjbg.log.collector.model.LogInfo;
import com.xjbg.log.collector.utils.JsonLogUtil;
import lombok.Getter;
import lombok.Setter;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;

import java.util.Date;
import java.util.List;

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
    protected void doLog(List<LogInfo> logInfos) throws Exception {
        BulkRequest bulkRequest = new BulkRequest();
        for (LogInfo logInfo : logInfos) {
            IndexRequest indexRequest = new IndexRequest();
            indexRequest.id(logInfo.getLogId()).index(getIndex()).type(MapperService.SINGLE_MAPPING_NAME).source(toJsonString(logInfo), Requests.INDEX_CONTENT_TYPE);
            bulkRequest.add(indexRequest);
        }
        BulkResponse bulk = getHighLevelClient().bulk(bulkRequest, RequestOptions.DEFAULT);
        if (bulk.hasFailures()) {
            throw new RuntimeException(bulk.buildFailureMessage());
        }
    }

    @Override
    public void cleanLog(Date before) throws Exception {
        log.info("clean up application[{}]'s log before:{}", LogCollectorConstant.APPLICATION, before);
        DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest();
        RangeQueryBuilder timeQuery = QueryBuilders.rangeQuery(getTimeFieldName()).lte(before);
        TermQueryBuilder applicationQuery = QueryBuilders.termQuery(JsonLogUtil.translate("application", getNamingStrategy()) + ".keyword", LogCollectorConstant.APPLICATION);
        QueryBuilder queryBuilder = QueryBuilders.boolQuery().filter(QueryBuilders.boolQuery().must(timeQuery).must(applicationQuery));
        deleteByQueryRequest.indices(getIndex());
        deleteByQueryRequest.setQuery(queryBuilder);
        getHighLevelClient().deleteByQuery(deleteByQueryRequest, RequestOptions.DEFAULT);
    }
}
