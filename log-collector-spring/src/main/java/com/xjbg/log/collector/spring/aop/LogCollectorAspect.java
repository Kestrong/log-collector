package com.xjbg.log.collector.spring.aop;

import com.xjbg.log.collector.properties.LogCollectorProperties;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.lang.NonNull;

/**
 * @author kesc
 * @since 2023-03-31 18:01
 */
@Slf4j
@Aspect
@Setter
public class LogCollectorAspect extends AbstractLogCollectorAspect {

    public LogCollectorAspect(LogCollectorProperties logCollectorProperties) {
        super(logCollectorProperties);
    }

    @Around(value = "log()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        return super.around(pjp);
    }

    @Override
    public Object invoke(@NonNull MethodInvocation invocation) throws Throwable {
        return super.invoke(invocation);
    }

}