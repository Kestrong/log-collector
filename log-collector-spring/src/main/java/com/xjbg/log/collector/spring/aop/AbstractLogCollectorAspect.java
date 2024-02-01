package com.xjbg.log.collector.spring.aop;

import com.xjbg.log.collector.LogCollectors;
import com.xjbg.log.collector.annotation.CollectorLog;
import com.xjbg.log.collector.api.LogCollector;
import com.xjbg.log.collector.api.impl.AbstractLogCollector;
import com.xjbg.log.collector.enums.LogState;
import com.xjbg.log.collector.model.LogInfo;
import com.xjbg.log.collector.properties.LogCollectorProperties;
import com.xjbg.log.collector.sensitive.SensitiveStrategy;
import com.xjbg.log.collector.spring.utils.LogSpringHttpRequestUtil;
import com.xjbg.log.collector.utils.ExceptionUtil;
import com.xjbg.log.collector.utils.JsonLogUtil;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author kesc
 * @since 2023-07-24 18:23
 */
public abstract class AbstractLogCollectorAspect implements MethodInterceptor {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected LogCollectorProperties logCollectorProperties;
    protected final ExpressionParser parser = new SpelExpressionParser();

    public AbstractLogCollectorAspect(LogCollectorProperties logCollectorProperties) {
        this.logCollectorProperties = logCollectorProperties;
    }

    protected boolean isEnable() {
        return logCollectorProperties.isEnable();
    }

    @Pointcut("@annotation(com.xjbg.log.collector.annotation.CollectorLog)||@within(com.xjbg.log.collector.annotation.CollectorLog)")
    public void log() {
    }

    @Before(value = "log()")
    public void doBefore() {
        log.debug("-----do before log-----");
    }

    protected void prepareLogRequest(Map<String, Object> logRequest, Parameter[] parameters, Object[] args) {
        if (parameters == null || parameters.length == 0) {
            return;
        }
        for (int i = 0, n = parameters.length; i < n; i++) {
            Object parameter = args != null && args.length - 1 >= i ? args[i] : null;
            if (getAnnotation(parameters[i].getAnnotations(), CollectorLog.Ignore.class) != null) {
                continue;
            }
            if (parameter instanceof HttpSession || parameter instanceof HttpServletRequest
                    || parameter instanceof HttpServletResponse) {
                continue;
            }
            String parameterName = parameters[i].getName();
            if (parameter instanceof MultipartFile) {
                logRequest.put(parameterName, ((MultipartFile) parameter).getOriginalFilename());
            } else {
                CollectorLog.Sensitive sensitive = parameters[i].getAnnotation(CollectorLog.Sensitive.class);
                if (sensitive != null && parameter instanceof String) {
                    logRequest.put(parameterName, sensitive.strategy().apply((String) parameter));
                } else {
                    logRequest.put(parameterName, parameter);
                }
            }
        }
    }

    @SuppressWarnings(value = {"unchecked", "rawtypes"})
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        if (!isEnable()) {
            return pjp.proceed();
        }
        log.debug("-----do around log-----");
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        CollectorLog collectorLog = getCollectorLogAnnotation(method);
        log.debug("collectorLog:{}", collectorLog);
        Object response = null;
        boolean fail = false;
        Map<String, Object> logRequest = new HashMap<>();
        LogInfo.LogInfoBuilder logInfoBuilder = LogInfo.builder().requestTime(new Date());
        try {
            prepareLogRequest(logRequest, method.getParameters(), pjp.getArgs());
            try {
                logInfoBuilder.params(toJson(logRequest));
            } catch (Exception e) {
                logInfoBuilder.params(ExceptionUtil.getTraceInfo(e));
            }
            Object proceed = pjp.proceed();
            if (!collectorLog.ignoreResponse()) {
                response = proceed;
            }
            return proceed;
        } catch (Throwable t) {
            fail = true;
            response = ExceptionUtil.getTraceInfo(t);
            throw t;
        } finally {
            if (!collectorLog.onError() || fail) {
                try {
                    EvaluationContext context = new StandardEvaluationContext();
                    if (response != null && !fail) {
                        context.setVariable("RESPONSE", response);
                    }
                    context.setVariable("SensitiveStrategy", SensitiveStrategy.class);
                    logRequest.forEach(context::setVariable);
                    logRequest.clear();
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
                    String message = getSpelValue(context, collectorLog.message());
                    if (StringUtils.isNotBlank(collectorLog.message())) {
                        logInfoBuilder.message(message);
                    }
                    LogInfo logInfo = logInfoBuilder.userAgent(LogSpringHttpRequestUtil.getUserAgent())
                            .requestIp(LogSpringHttpRequestUtil.getRequestIp()).requestUrl(LogSpringHttpRequestUtil.getRequestURL()).requestMethod(LogSpringHttpRequestUtil.getRequestMethod())
                            .state(fail ? LogState.FAIL.name() : LogState.SUCCESS.name()).response(toJson(response)).build();
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
        }
    }

    @Override
    @SuppressWarnings(value = {"unchecked"})
    public Object invoke(@NonNull MethodInvocation invocation) throws Throwable {
        if (!isEnable() || getCollectorLogAnnotation(invocation.getMethod()) != null || invocation.getMethod().isAnnotationPresent(CollectorLog.Ignore.class)) {
            return invocation.proceed();
        }
        log.debug("-----do log MethodInterceptor invoke-----");
        Object response = null;
        boolean fail = false;
        Map<String, Object> logRequest = new HashMap<>();
        LogInfo.LogInfoBuilder logInfoBuilder = LogInfo.builder().requestTime(new Date());
        try {
            prepareLogRequest(logRequest, invocation.getMethod().getParameters(), invocation.getArguments());
            logInfoBuilder.handleMethod(invocation.getThis() == null ? invocation.getMethod().getDeclaringClass().getName() : invocation.getThis().getClass().getName() + "#" + invocation.getMethod().getName())
                    .requestIp(LogSpringHttpRequestUtil.getRequestIp()).userAgent(LogSpringHttpRequestUtil.getUserAgent())
                    .requestUrl(LogSpringHttpRequestUtil.getRequestURL()).requestMethod(LogSpringHttpRequestUtil.getRequestMethod());
            try {
                logInfoBuilder.params(toJson(logRequest));
            } catch (Exception e) {
                logInfoBuilder.params(ExceptionUtil.getTraceInfo(e));
            }
            response = invocation.proceed();
            return response;
        } catch (Throwable t) {
            fail = true;
            response = ExceptionUtil.getTraceInfo(t);
            throw t;
        } finally {
            try {
                LogInfo logInfo = logInfoBuilder.state(fail ? LogState.FAIL.name() : LogState.SUCCESS.name())
                        .response(toJson(response)).responseTime(new Date()).build();
                LogCollectors.defaultCollector().logAsync(logInfo);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    @After(value = "log()")
    public void doAfter() {
        log.debug("-----do after log-----");
    }

    protected String toJson(Object o) {
        return JsonLogUtil.toJson(o);
    }

    protected CollectorLog getCollectorLogAnnotation(Method method) {
        CollectorLog annotation = method.getAnnotation(CollectorLog.class);
        if (annotation == null) {
            annotation = method.getDeclaringClass().getAnnotation(CollectorLog.class);
        }
        return annotation;
    }

    @SuppressWarnings("all")
    protected <T extends Annotation> T getAnnotation(Annotation[] annotations, Class<T> target) {
        if (annotations != null && annotations.length > 0) {
            for (Annotation annotation : annotations) {
                if (target.isInstance(annotation)) {
                    return (T) annotation;
                }
            }
        }
        return null;
    }

    protected String getSpelValue(EvaluationContext context, String template) {
        if (StringUtils.isBlank(template)) {
            return null;
        }
        if (!template.startsWith("#")) {
            return template;
        }
        try {
            Expression expression = parser.parseExpression(template);
            return String.valueOf(expression.getValue(context));
        } catch (Exception e) {
            log.error(e.getMessage());
            return template;
        }
    }

}
