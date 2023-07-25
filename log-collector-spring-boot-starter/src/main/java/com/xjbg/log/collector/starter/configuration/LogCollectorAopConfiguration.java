package com.xjbg.log.collector.starter.configuration;

import com.xjbg.log.collector.spring.aop.LogCollectorAspect;
import com.xjbg.log.collector.properties.LogCollectorProperties;
import com.xjbg.log.collector.spring.aop.ReactiveLogCollectorAspect;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author kesc
 * @since 2023-04-18 16:02
 */
@Configuration
public class LogCollectorAopConfiguration {

    @Bean
    @RefreshScope
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = LogCollectorProperties.PREFIX)
    public LogCollectorProperties logCollectorProperties() {
        return new LogCollectorProperties();
    }

    @Bean(name = "logCollectorAspect")
    @ConditionalOnMissingBean(name = "logCollectorAspect")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public LogCollectorAspect logCollectorAspect() {
        return new LogCollectorAspect(logCollectorProperties());
    }

    @Bean(name = "logCollectorDefaultPointcutAdvisor")
    @ConditionalOnMissingBean(name = "logCollectorDefaultPointcutAdvisor")
    @ConditionalOnProperty(name = LogCollectorProperties.PREFIX + ".pointcut")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public DefaultPointcutAdvisor logCollectorDefaultPointcutAdvisor() {
        DefaultPointcutAdvisor pointcutAdvisor = new DefaultPointcutAdvisor();
        AspectJExpressionPointcut aspectJExpressionPointcut = new AspectJExpressionPointcut();
        aspectJExpressionPointcut.setExpression(logCollectorProperties().getPointcut());
        pointcutAdvisor.setPointcut(aspectJExpressionPointcut);
        pointcutAdvisor.setAdvice(logCollectorAspect());
        return pointcutAdvisor;
    }

    @Bean(name = "reactiveLogCollectorAspect")
    @ConditionalOnMissingBean(name = "reactiveLogCollectorAspect")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public ReactiveLogCollectorAspect reactiveLogCollectorAspect() {
        return new ReactiveLogCollectorAspect(logCollectorProperties());
    }

    @Bean(name = "reactiveLogCollectorDefaultPointcutAdvisor")
    @ConditionalOnMissingBean(name = "reactiveLogCollectorDefaultPointcutAdvisor")
    @ConditionalOnProperty(name = LogCollectorProperties.PREFIX + ".pointcut")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public DefaultPointcutAdvisor reactiveLogCollectorDefaultPointcutAdvisor() {
        DefaultPointcutAdvisor pointcutAdvisor = new DefaultPointcutAdvisor();
        AspectJExpressionPointcut aspectJExpressionPointcut = new AspectJExpressionPointcut();
        aspectJExpressionPointcut.setExpression(logCollectorProperties().getPointcut());
        pointcutAdvisor.setPointcut(aspectJExpressionPointcut);
        pointcutAdvisor.setAdvice(reactiveLogCollectorAspect());
        return pointcutAdvisor;
    }
}
