package com.xjbg.log.collector.api.impl;

import com.xjbg.log.collector.enums.CollectorType;
import com.xjbg.log.collector.model.LogInfo;
import com.xjbg.log.collector.utils.JsonLogUtil;
import lombok.Getter;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.List;

@Getter
public class KafkaLogCollector extends AbstractLogCollector<LogInfo, LogInfo> {
    private final Producer<String, String> producer;

    public KafkaLogCollector(Producer<String, String> producer) {
        this.producer = producer;
    }

    @Override
    protected void doLog(List<LogInfo> logInfos) throws Exception {
        for (LogInfo logInfo : logInfos) {
            ProducerRecord<String, String> record = new ProducerRecord<>(getTopic(), logInfo.getLogId(), JsonLogUtil.toJson(logInfo));
            producer.send(record, (metadata, ex) -> {
                if (ex != null) {
                    log.error("failed send log to topic:{}, partition: {}, offset: {}", metadata.topic(), metadata.partition(), metadata.offset(), ex);
                    logAsyncFallback(logInfo);
                } else {
                    log.debug("successful send log to topic:{}, partition: {}, offset: {}", metadata.topic(), metadata.partition(), metadata.offset());
                }
            });
        }
    }

    @Override
    public String type() {
        return CollectorType.KAFKA.getType();
    }

}