package com.xjbg.log.collector.utils;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.xjbg.log.collector.enums.NamingStrategy;
import com.xjbg.log.collector.sensitive.SensitiveLogAnnotationIntrospector;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;

/**
 * @author kesc
 * @since 2023-04-03 15:29
 */
@Slf4j
public class JsonLogUtil {
    private static final ObjectMapper objectMapper = createObjectMapper();

    private static void configure(ObjectMapper objectMapper) {
        try {
            objectMapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);
            objectMapper.configure(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL, true);
        } catch (Error e) {
            //since 2.11
            log.error("SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL not support your version, please upgrade jackson >=2.11");
        }
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        SimpleModule module = new SimpleModule();
        module.addSerializer(BigDecimal.class, ToStringSerializer.instance);
        objectMapper.registerModules(module);
        AnnotationIntrospector ai = objectMapper.getSerializationConfig().getAnnotationIntrospector();
        AnnotationIntrospector newAi = AnnotationIntrospectorPair.pair(ai, new SensitiveLogAnnotationIntrospector());
        objectMapper.setAnnotationIntrospector(newAi);
        try {
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE);
        } catch (Error e) {
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);
        }
    }

    public static ObjectMapper getDefaultObjectMapper() {
        return objectMapper;
    }

    public static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        configure(objectMapper);
        return objectMapper;
    }

    @SneakyThrows
    public static String toJson(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof String) {
            return (String) o;
        }
        return objectMapper.writeValueAsString(o);
    }

    public static String translate(String name, NamingStrategy namingStrategy) {
        String translateName;
        switch (namingStrategy) {
            case UPPER_CAMEL_CASE:
                try {
                    translateName = PropertyNamingStrategies.UpperCamelCaseStrategy.INSTANCE.translate(name);
                } catch (Error e) {
                    translateName = ((PropertyNamingStrategy.UpperCamelCaseStrategy) PropertyNamingStrategy.UPPER_CAMEL_CASE).translate(name);
                }
                break;
            case UPPER_SNAKE_CASE:
                try {
                    translateName = PropertyNamingStrategies.UpperSnakeCaseStrategy.INSTANCE.translate(name);
                } catch (Error e) {
                    translateName = ((PropertyNamingStrategy.SnakeCaseStrategy) PropertyNamingStrategy.SNAKE_CASE).translate(name);
                    if (translateName != null) {
                        translateName = translateName.toUpperCase();
                    }
                }
                break;
            case SNAKE_CASE:
                try {
                    translateName = PropertyNamingStrategies.SnakeCaseStrategy.INSTANCE.translate(name);
                } catch (Error e) {
                    translateName = ((PropertyNamingStrategy.SnakeCaseStrategy) PropertyNamingStrategy.SNAKE_CASE).translate(name);
                }
                break;
            case KEBAB_CASE:
                try {
                    translateName = PropertyNamingStrategies.KebabCaseStrategy.INSTANCE.translate(name);
                } catch (Error e) {
                    translateName = ((PropertyNamingStrategy.KebabCaseStrategy) PropertyNamingStrategy.KEBAB_CASE).translate(name);
                }
                break;
            case LOWER_CASE:
                try {
                    translateName = PropertyNamingStrategies.LowerCaseStrategy.INSTANCE.translate(name);
                } catch (Error e) {
                    translateName = ((PropertyNamingStrategy.LowerCaseStrategy) PropertyNamingStrategy.LOWER_CASE).translate(name);
                }
                break;
            case LOWER_DOT_CASE:
                try {
                    translateName = PropertyNamingStrategies.LowerDotCaseStrategy.INSTANCE.translate(name);
                } catch (Error e) {
                    translateName = ((PropertyNamingStrategy.LowerDotCaseStrategy) PropertyNamingStrategy.LOWER_DOT_CASE).translate(name);
                }
                break;
            default:
                try {
                    translateName = PropertyNamingStrategies.LowerCamelCaseStrategy.INSTANCE.translate(name);
                } catch (Error e) {
                    translateName = PropertyNamingStrategy.LOWER_CAMEL_CASE.nameForField(null, null, name);
                }
                break;
        }
        return translateName;
    }

}
