package com.xjbg.log.collector.api.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.json.JsonData;
import com.xjbg.log.collector.LogCollectorConstant;
import com.xjbg.log.collector.model.LogInfo;
import com.xjbg.log.collector.utils.JsonLogUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
        BulkResponse bulk = getElasticsearchClient().bulk(new BulkRequest.Builder().index(getIndex()).operations(bulkOperations).build());
        if (bulk.errors()) {
            throw new RuntimeException(bulk.items().stream().filter(x -> x.error() != null).map(x -> x.error().reason()).collect(Collectors.joining(",")));
        }
    }

    @Override
    public void cleanLog(Date before) throws Exception {
        log.info("clean up application[{}]'s log before:{}", LogCollectorConstant.APPLICATION, before);
        Query timeQuery = RangeQuery.of(t -> t.field(getTimeFieldName()).lte(JsonData.of(before)))._toQuery();
        Query applicationQuery = TermQuery.of(t -> t.field(JsonLogUtil.translate("application", getNamingStrategy()) + ".keyword").value(LogCollectorConstant.APPLICATION))._toQuery();
        Query query = BoolQuery.of(b -> b.filter(timeQuery, applicationQuery))._toQuery();
        DeleteByQueryRequest deleteByQueryRequest = new DeleteByQueryRequest.Builder().index(getIndex()).query(query).build();
        getElasticsearchClient().deleteByQuery(deleteByQueryRequest);
    }

}
