package com.xjbg.log.collector.starter.tests;

import com.xjbg.log.collector.LogCollectors;
import com.xjbg.log.collector.api.LogCollector;
import com.xjbg.log.collector.model.LogInfo;
import com.xjbg.log.collector.starter.base.BaseTest;
import com.xjbg.log.collector.starter.example.UserInfo;
import com.xjbg.log.collector.starter.example.UserService;
import com.xjbg.log.collector.starter.example.feign.UserFeignClient;
import com.xjbg.log.collector.utils.JsonLogUtil;
import org.junit.Test;
import org.openjdk.jol.info.GraphStatsWalker;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * @author kesc
 * @since 2023-04-07 17:16
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class LogCollectorTest extends BaseTest {
    @Autowired
    private UserService userService;
    @Autowired(required = false)
    private UserFeignClient userFeignClient;

    private UserInfo createUserInfo() {
        return UserInfo.builder()
                .realName("柯某")
                .userName("abcdefg")
                .password("123456")
                .telephone("11259661345")
                .address("福建省厦门市思明区福满山庄")
                .idCard("492847711903621035")
                .bankCard("4928477119036210351")
                .email("abc@foxmail.com").money("100").birthday(new Date()).build();
    }

    @Test
    public void testLogCollectorAop() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            userService.addUser(createUserInfo());
        }
        Thread.sleep(30_000L);
    }

    @Test
    public void testLogCollectorManual() throws InterruptedException {
        LogCollector logCollector = LogCollectors.defaultCollector();
        for (int i = 0; i < 100; i++) {
            logCollector.logAsync(LogInfo.builder().params(JsonLogUtil.toJson(createUserInfo())).build());
        }
        Thread.sleep(30_000L);
    }

    @Test
    public void testLogCollectorFeign() throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            userFeignClient.userInfo(createUserInfo());
        }
        Thread.sleep(30_000L);
    }

    @Test
    public void testObjectSizeCalculate() {
        GraphStatsWalker graphStatsWalker = new GraphStatsWalker();
        System.out.println(graphStatsWalker.walk(createUserInfo()).totalSize());
    }
}
