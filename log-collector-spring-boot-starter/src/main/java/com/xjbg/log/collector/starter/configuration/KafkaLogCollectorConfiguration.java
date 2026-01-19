package com.xjbg.log.collector.starter.configuration;

import com.xjbg.log.collector.api.impl.KafkaLogCollector;
import com.xjbg.log.collector.properties.LogCollectorProperties;
import com.xjbg.log.collector.starter.autoconfig.LogCollectorAutoConfiguration;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@ConditionalOnClass(value = {KafkaTemplate.class, KafkaProducer.class})
@Configuration
public class KafkaLogCollectorConfiguration {
    @Autowired
    private LogCollectorAutoConfiguration logCollectorAutoConfiguration;
    @Autowired
    private LogCollectorProperties properties;

    @Bean(name = "kafkaLogProducer")
    @ConditionalOnMissingBean(name = "kafkaLogProducer")
    @ConditionalOnProperty(name = LogCollectorProperties.PREFIX + ".kafka.enable", havingValue = "true")
    @SuppressWarnings("unchecked")
    public Producer<String, String> kafkaLogProducer(KafkaTemplate<?, ?> kafkaTemplate) {
        ProducerFactory<?, ?> producerFactory = kafkaTemplate.getProducerFactory();
        Map<String, Object> configurationProperties = producerFactory.getConfigurationProperties();
        Object keySerializer = configurationProperties.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG);
        Object valueSerializer = configurationProperties.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG);
        if (keySerializer.equals(StringSerializer.class) && valueSerializer.equals(StringSerializer.class)) {
            return (Producer<String, String>) kafkaTemplate.getProducerFactory().createProducer();
        }
        Map<String, Object> overrideProperties = new HashMap<>();
        overrideProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        overrideProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        ProducerFactory<?, ?> copyProducerFactory = producerFactory.copyWithConfigurationOverride(overrideProperties);
        return (Producer<String, String>) copyProducerFactory.createProducer();
    }

    @Bean(value = "kafkaLogCollector", initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(name = LogCollectorProperties.PREFIX + ".kafka.enable", havingValue = "true")
    @ConditionalOnMissingBean(name = "kafkaLogCollector")
    public KafkaLogCollector kafkaLogCollector(Producer<String, String> kafkaLogProducer) throws ReflectiveOperationException {
        KafkaLogCollector kafkaLogCollector = new KafkaLogCollector(kafkaLogProducer);
        LogCollectorProperties.KafkaLogCollectorCustomProperties propertiesKafka = properties.getKafka();
        logCollectorAutoConfiguration.setGlobalProperties(kafkaLogCollector, properties);
        logCollectorAutoConfiguration.setCustomProperties(kafkaLogCollector, propertiesKafka);
        if (StringUtils.hasText(propertiesKafka.getTopic())) {
            kafkaLogCollector.setTopic(propertiesKafka.getTopic());
        }
        return kafkaLogCollector;
    }

}
