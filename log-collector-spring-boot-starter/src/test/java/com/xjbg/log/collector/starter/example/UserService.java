package com.xjbg.log.collector.starter.example;

import com.xjbg.log.collector.annotation.CollectorLog;
import org.springframework.stereotype.Service;

/**
 * @author kesc
 * @since 2023-04-13 15:12
 */
@Service
public class UserService {

    @CollectorLog(businessNo = "#SensitiveStrategy.ACCESS_KEY.apply(#userInfo.userName)")
    public void addUser(UserInfo userInfo) {

    }

}
