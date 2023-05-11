package com.xjbg.log.collector.api.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.json.JsonData;
import com.xjbg.log.collector.model.LogInfo;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author kesc
 * @since 2023-04-07 10:57
 */
@Getter
@Setter
public class Es8LogCollector extends AbstractEsLogCollector {
    private ElasticsearchClient elasticsearchClient;

    public Es8LogCollector(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @Override
    protected void doLog(List<LogInfo> logInfos) throws Exception {
        List<BulkOperation> bulkOperations = new ArrayList<>();
        for (LogInfo logInfo : logInfos) {
            BulkOperation bulkOperation = new BulkOperation.Builder().create(d -> d.document(logInfo).id(logInfo.getLogId())).build();
            bulkOperations.add(bulkOperation);
        }
        getElasticsearchClient().bulk(new BulkRequest.Builder().index(getIndex()).operations(bulkOperations).build());
    }

    @Override
    public void cleanLog(Date before) throws Exception {
        log.info("clean up log before:{}", before);
        DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest.Builder().index(getIndex()).query(RangeQuery.of(t -> t.field(getTimeFieldName()).lte(JsonData.of(before)))._toQuery()).build();
        getElasticsearchClient().deleteByQuery(deleteByQueryRequest);
    }

}
