package com.xjbg.log.collector.starter.feign;

import com.xjbg.log.collector.properties.LogCollectorProperties;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author kesc
 * @since 2023-04-20 9:25
 */
public interface FeignLogCollectorClient {

    @RequestMapping(value = "${" + LogCollectorProperties.PREFIX + ".feign.path}", method = {RequestMethod.POST})
    Object sendLogPost(@RequestBody Object logInfo);

    @RequestMapping(value = "${" + LogCollectorProperties.PREFIX + ".feign.path}", method = {RequestMethod.PUT})
    Object sendLogPut(@RequestBody Object logInfo);

}
