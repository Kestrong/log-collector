package com.xjbg.log.collector.spring.aop;

import com.xjbg.log.collector.LogCollectors;
import com.xjbg.log.collector.annotation.CollectorLog;
import com.xjbg.log.collector.api.LogCollector;
import com.xjbg.log.collector.api.impl.AbstractLogCollector;
import com.xjbg.log.collector.enums.LogState;
import com.xjbg.log.collector.model.LogInfo;
import com.xjbg.log.collector.properties.LogCollectorProperties;
import com.xjbg.log.collector.sensitive.SensitiveStrategy;
import com.xjbg.log.collector.spring.utils.ReactiveLogHttpRequestUtil;
import com.xjbg.log.collector.spring.utils.ReactiveRequestContextHolder;
import com.xjbg.log.collector.utils.ExceptionUtil;
import com.xjbg.log.collector.utils.ReactiveLogContextHolder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author kesc
 * @since 2023-07-24 14:17
 */
@Slf4j
@Aspect
@Setter
@SuppressWarnings(value = {"all"})
public class ReactiveLogCollectorAspect extends AbstractLogCollectorAspect implements Ordered {

    public ReactiveLogCollectorAspect(LogCollectorProperties logCollectorProperties) {
        super(logCollectorProperties);
    }

    protected boolean hasMonoArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return false;
        }
        for (Object arg : args) {
            if (arg instanceof Mono || arg instanceof Flux) {
                return true;
            }
        }
        return false;
    }

    @Around(value = "log()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        if (!isEnable()) {
            return pjp.proceed();
        }
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Class<?> returnType = method.getReturnType();
        if (!returnType.isAssignableFrom(Mono.class) && !returnType.isAssignableFrom(Flux.class)) {
            if (!hasMonoArgs(pjp.getArgs())) {
                return super.around(pjp);
            }
            log.warn("log collector aop only support return type Mono or Flux");
            return pjp.proceed();
        }
        log.debug("-----do around log-----");
        CollectorLog collectorLog = getCollectorLogAnnotation(method);
        log.debug("collectorLog:{}", collectorLog);
        Map<String, Object> logRequest = new HashMap<>();
        LogInfo.LogInfoBuilder logInfoBuilder = LogInfo.builder().state(LogState.SUCCESS.name()).requestTime(new Date());
        prepareLogRequest(logRequest, method.getParameters(), pjp.getArgs());
        EvaluationContext context = new StandardEvaluationContext();
        context.setVariable("SensitiveStrategy", SensitiveStrategy.class);
        Mono<Object> mono = ReactiveLogContextHolder.getContext().flatMap(x -> {
                    logInfoBuilder.userId(x.getUserId()).requestId(x.getRequestId());
                    return Mono.just(x);
                }).flatMap(x -> ReactiveRequestContextHolder.getRequestContext())
                .flatMap(x -> {
                    logInfoBuilder.userAgent(ReactiveLogHttpRequestUtil.getUserAgent(x.getRequest()))
                            .requestIp(ReactiveLogHttpRequestUtil.getRequestIp(x.getRequest())).
                            requestUrl(ReactiveLogHttpRequestUtil.getRequestURL(x.getRequest()))
                            .requestMethod(ReactiveLogHttpRequestUtil.getRequestMethod(x.getRequest()));
                    return Mono.just(x);
                }).flatMap(x -> {
                    Map<String, Object> param = new HashMap<>();
                    logRequest.forEach((k, v) -> {
                        if (v instanceof Mono) {
                            ((Mono<?>) v).subscribe(z -> param.put(k, z));
                        } else if (v instanceof Flux) {
                            ((Flux<?>) v).subscribe(z -> param.put(k, z));
                        } else {
                            param.put(k, v);
                        }
                    });
                    if (StringUtils.isBlank(collectorLog.detail())) {
                        try {
                            logInfoBuilder.params(toJson(param));
                        } catch (Exception e) {
                            logInfoBuilder.params(ExceptionUtil.getTraceInfo(e));
                        }
                    }
                    param.forEach(context::setVariable);
                    logRequest.clear();
                    param.clear();
                    return Mono.just(param);
                });
        Consumer<SignalType> finalConsumer = signalType -> {
            boolean fail = SignalType.ON_ERROR.equals(signalType);
            if (!collectorLog.onError() || fail) {
                try {
                    logInfoBuilder.responseTime(new Date())
                            .businessNo(getSpelValue(context, collectorLog.businessNo()))
                            .module(getSpelValue(context, collectorLog.module()))
                            .action(getSpelValue(context, collectorLog.action()))
                            .type(getSpelValue(context, collectorLog.type()));
                    String requestMethod = getSpelValue(context, collectorLog.handleMethod());
                    if (StringUtils.isBlank(requestMethod)) {
                        logInfoBuilder.handleMethod(pjp.getTarget().getClass().getName() + "#" + method.getName());
                    } else {
                        logInfoBuilder.handleMethod(pjp.getTarget().getClass().getName() + "#" + requestMethod);
                    }
                    String detail = getSpelValue(context, collectorLog.detail());
                    if (StringUtils.isNotBlank(collectorLog.detail())) {
                        logInfoBuilder.params(detail);
                    }
                    LogInfo logInfo = logInfoBuilder.build();
                    LogCollector logCollector = collectorLog.collector().isAssignableFrom(AbstractLogCollector.None.class) ? LogCollectors.defaultCollector() : LogCollectors.getCollector(collectorLog.collector());
                    if (collectorLog.async()) {
                        logCollector.logAsync(logInfo);
                    } else {
                        logCollector.log(logInfo);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }
        };
        if (returnType.isAssignableFrom(Mono.class)) {
            try {
                Mono proceed = (Mono) pjp.proceed();
                return mono.flatMap(x -> proceed).flatMap(x -> {
                    if (StringUtils.isNotBlank(collectorLog.detail())) {
                        logInfoBuilder.params(collectorLog.detail());
                    } else {
                        if (!collectorLog.ignoreResponse()) {
                            logInfoBuilder.response(toJson(x));
                        }
                    }
                    context.setVariable("RESPONSE", x);
                    return Mono.just(x);
                }).doOnError((Consumer<Throwable>) throwable -> {
                    logInfoBuilder.state(LogState.FAIL.name()).response(ExceptionUtil.getTraceInfo(throwable));
                }).doFinally(finalConsumer);
            } catch (Exception e) {
                return mono.flatMap(x -> {
                    return Mono.error(e).doFinally(y -> {
                        logInfoBuilder.state(LogState.FAIL.name()).response(ExceptionUtil.getTraceInfo(e));
                        finalConsumer.accept(SignalType.ON_ERROR);
                    });
                });
            }
        } else {
            try {
                Flux proceed = (Flux) pjp.proceed();
                return mono.flatMapMany(x -> proceed).flatMap(x -> {
                    if (!collectorLog.ignoreResponse()) {
                        logInfoBuilder.response(toJson(x));
                    }
                    context.setVariable("RESPONSE", x);
                    return Flux.just(x);
                }).doOnError((Consumer<Throwable>) throwable -> {
                    logInfoBuilder.state(LogState.FAIL.name()).response(ExceptionUtil.getTraceInfo(throwable));
                }).doFinally(finalConsumer);
            } catch (Exception e) {
                return mono.flatMapMany(x -> {
                    return Flux.error(e).doFinally(y -> {
                        logInfoBuilder.state(LogState.FAIL.name()).response(ExceptionUtil.getTraceInfo(e));
                        finalConsumer.accept(SignalType.ON_ERROR);
                    });
                });
            }
        }
    }

    @Override
    public Object invoke(@NonNull MethodInvocation invocation) throws Throwable {
        if (!isEnable() || getCollectorLogAnnotation(invocation.getMethod()) != null || invocation.getMethod().isAnnotationPresent(CollectorLog.Ignore.class)) {
            return invocation.proceed();
        }
        Class<?> returnType = invocation.getMethod().getReturnType();
        if (!returnType.isAssignableFrom(Mono.class) && !returnType.isAssignableFrom(Flux.class)) {
            if (!hasMonoArgs(invocation.getArguments())) {
                return super.invoke(invocation);
            }
            log.warn("log collector aop only support return type Mono or Flux");
            return invocation.proceed();
        }
        log.debug("-----do log MethodInterceptor invoke-----");
        Map<String, Object> logRequest = new HashMap<>();
        LogInfo.LogInfoBuilder logInfoBuilder = LogInfo.builder().state(LogState.SUCCESS.name()).requestTime(new Date());
        prepareLogRequest(logRequest, invocation.getMethod().getParameters(), invocation.getArguments());
        Mono<Object> mono = ReactiveLogContextHolder.getContext().flatMap(x -> {
                    logInfoBuilder.userId(x.getUserId()).requestId(x.getRequestId());
                    return Mono.just(x);
                }).flatMap(x -> ReactiveRequestContextHolder.getRequestContext())
                .flatMap(x -> {
                    logInfoBuilder.userAgent(ReactiveLogHttpRequestUtil.getUserAgent(x.getRequest()))
                            .requestIp(ReactiveLogHttpRequestUtil.getRequestIp(x.getRequest())).
                            requestUrl(ReactiveLogHttpRequestUtil.getRequestURL(x.getRequest()))
                            .requestMethod(ReactiveLogHttpRequestUtil.getRequestMethod(x.getRequest()));
                    return Mono.just(x);
                }).flatMap(x -> {
                    Map<String, Object> param = new HashMap<>();
                    logRequest.forEach((k, v) -> {
                        if (v instanceof Mono) {
                            ((Mono<?>) v).subscribe(z -> param.put(k, z));
                        } else if (v instanceof Flux) {
                            ((Flux<?>) v).subscribe(z -> param.put(k, z));
                        } else {
                            param.put(k, v);
                        }
                    });
                    try {
                        logInfoBuilder.params(toJson(param));
                    } catch (Exception e) {
                        logInfoBuilder.params(ExceptionUtil.getTraceInfo(e));
                    }
                    logInfoBuilder.handleMethod(invocation.getThis() == null ? invocation.getMethod().getDeclaringClass().getName() : invocation.getThis().getClass().getName() + "#" + invocation.getMethod().getName());
                    logRequest.clear();
                    param.clear();
                    return Mono.just(param);
                });
        Consumer<SignalType> finalConsumer = signalType -> {
            try {
                boolean fail = SignalType.ON_ERROR.equals(signalType);
                LogInfo logInfo = logInfoBuilder.responseTime(new Date()).build();
                LogCollectors.defaultCollector().logAsync(logInfo);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        };
        if (returnType.isAssignableFrom(Mono.class)) {
            try {
                Mono proceed = (Mono) invocation.proceed();
                return mono.flatMap(x -> proceed).flatMap(x -> {
                    logInfoBuilder.response(toJson(x));
                    return Mono.just(x);
                }).doOnError((Consumer<Throwable>) throwable -> {
                    logInfoBuilder.state(LogState.FAIL.name()).response(ExceptionUtil.getTraceInfo(throwable));
                }).doFinally(finalConsumer);
            } catch (Exception e) {
                return mono.flatMap(x -> {
                    return Mono.error(e).doFinally(y -> {
                        logInfoBuilder.state(LogState.FAIL.name()).response(ExceptionUtil.getTraceInfo(e));
                        finalConsumer.accept(SignalType.ON_ERROR);
                    });
                });
            }
        } else {
            try {
                Flux proceed = (Flux) invocation.proceed();
                return mono.flatMapMany(x -> proceed).flatMap(x -> {
                    logInfoBuilder.response(toJson(x));
                    return Mono.just(x);
                }).doOnError((Consumer<Throwable>) throwable -> {
                    logInfoBuilder.state(LogState.FAIL.name()).response(ExceptionUtil.getTraceInfo(throwable));
                }).doFinally(finalConsumer);
            } catch (Exception e) {
                return mono.flatMapMany(x -> {
                    return Flux.error(e).doFinally(y -> {
                        logInfoBuilder.state(LogState.FAIL.name()).response(ExceptionUtil.getTraceInfo(e));
                        finalConsumer.accept(SignalType.ON_ERROR);
                    });
                });
            }
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
