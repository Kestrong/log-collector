package com.xjbg.log.collector.starter.example;

import com.xjbg.log.collector.annotation.CollectorLog;
import com.xjbg.log.collector.sensitive.SensitiveStrategy;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * @author kesc
 * @since 2023-04-12 15:43
 */
@Data
@Builder
public class UserInfo {
    @CollectorLog.Sensitive(strategy = SensitiveStrategy.REAL_NAME)
    private String realName;
    @CollectorLog.Sensitive(strategy = SensitiveStrategy.ACCESS_KEY)
    private String userName;
    @CollectorLog.Sensitive(strategy = SensitiveStrategy.SECRET_KEY)
    private String password;
    @CollectorLog.Sensitive(strategy = SensitiveStrategy.ADDRESS)
    private String address;
    @CollectorLog.Sensitive(strategy = SensitiveStrategy.ID_CARD)
    private String idCard;
    @CollectorLog.Sensitive(strategy = SensitiveStrategy.BANK_CARD)
    private String bankCard;
    @CollectorLog.Sensitive(strategy = SensitiveStrategy.EMAIL)
    private String email;
    @CollectorLog.Sensitive(strategy = SensitiveStrategy.PHONE)
    private String telephone;
    @CollectorLog.Ignore
    private String money;
    private Date birthday;
}
