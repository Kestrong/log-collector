package com.xjbg.log.collector.starter.base;

import com.xjbg.log.collector.starter.LogCollectorTestApplication;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author kesc
 * @since 2023-04-07 16:19
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = LogCollectorTestApplication.class)
@ActiveProfiles(value = "local")
public abstract class BaseTest {

}
