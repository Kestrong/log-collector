package com.xjbg.log.collector.starter.example.feign;

import com.xjbg.log.collector.starter.example.UserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author kesc
 * @since 2023-04-17 10:45
 */
@FeignClient(value = "test-log-collector", url = "http://localhost:8080")
public interface UserFeignClient {

    @PostMapping(value = "/user")
    void userInfo(@RequestBody UserInfo userInfo);

}
