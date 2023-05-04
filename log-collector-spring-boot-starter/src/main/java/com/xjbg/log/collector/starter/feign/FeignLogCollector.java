package com.xjbg.log.collector.starter.feign;

import com.xjbg.log.collector.api.impl.AbstractLogCollector;
import com.xjbg.log.collector.enums.CollectorType;
import com.xjbg.log.collector.model.LogInfo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author kesc
 * @since 2023-04-20 9:24
 */
@Setter
@Getter
public class FeignLogCollector extends AbstractLogCollector<LogInfo, Object> {
    private final FeignLogCollectorClient feignLogCollectorClient;
    private String method = RequestMethod.POST.name();

    public FeignLogCollector(FeignLogCollectorClient feignLogCollectorClient) {
        this.feignLogCollectorClient = feignLogCollectorClient;
    }

    @Override
    public String type() {
        return CollectorType.FEIGN.getType();
    }

    @Override
    protected void doLog(Object logInfo) throws Exception {
        if (RequestMethod.PUT.name().equalsIgnoreCase(method)) {
            feignLogCollectorClient.sendLogPut(logInfo);
        } else {
            feignLogCollectorClient.sendLogPost(logInfo);
        }
        log.debug("send log to {}, body:{}", feignLogCollectorClient, logInfo);
    }
}
