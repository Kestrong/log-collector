package com.xjbg.log.collector.sensitive;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author kesc
 * @since 2023-04-12 11:25
 */
public class SensitiveJsonSerializer extends JsonSerializer<String> {
    private static final Map<SensitiveStrategy, SensitiveJsonSerializer> instances = new ConcurrentHashMap<>();
    private final SensitiveStrategy strategy;

    private SensitiveJsonSerializer(SensitiveStrategy strategy) {
        this.strategy = strategy;
    }

    public static SensitiveJsonSerializer create(SensitiveStrategy strategy) {
        return instances.computeIfAbsent(strategy, SensitiveJsonSerializer::new);
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (strategy != null && value != null) {
            gen.writeString(strategy.apply(value));
        } else {
            gen.writeString(value);
        }
    }

}
