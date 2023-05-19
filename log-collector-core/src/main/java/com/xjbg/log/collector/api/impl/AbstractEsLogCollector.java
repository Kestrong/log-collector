package com.xjbg.log.collector.api.impl;

import com.xjbg.log.collector.enums.CollectorType;
import com.xjbg.log.collector.enums.NamingStrategy;
import com.xjbg.log.collector.model.LogInfo;
import com.xjbg.log.collector.utils.JsonLogUtil;
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
        return JsonLogUtil.translate("createTime", getNamingStrategy());
    }

}
