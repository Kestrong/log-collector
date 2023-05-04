package com.xjbg.log.collector.api.impl;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.xjbg.log.collector.enums.CollectorType;
import com.xjbg.log.collector.enums.NamingStrategy;
import com.xjbg.log.collector.model.LogInfo;
import lombok.Getter;
import lombok.Setter;

/**
 * @author kesc
 * @since 2023-04-11 10:20
 */
@Getter
@Setter
public abstract class AbstractEsLogCollector extends AbstractJsonLogCollector<LogInfo, LogInfo> {
    private String index = "log_info_index";
    private NamingStrategy namingStrategy = NamingStrategy.CAMEL_CASE;

    @Override
    public String type() {
        return CollectorType.ES.getType();
    }

    protected String getTimeFieldName() {
        String timeField;
        switch (getNamingStrategy()) {
            case UPPER_CAMEL_CASE:
                timeField = "CreateTime";
                break;
            case UPPER_SNAKE_CASE:
                try {
                    PropertyNamingStrategy upperSnakeCase = PropertyNamingStrategies.UPPER_SNAKE_CASE;
                    timeField = "CREATE_TIME";
                    break;
                } catch (Error e) {
                    //ignore
                }
            case SNAKE_CASE:
                timeField = "create_time";
                break;
            case KEBAB_CASE:
                timeField = "create-time";
                break;
            default:
                timeField = "createTime";
                break;
        }
        return timeField;
    }

}
