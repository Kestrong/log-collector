package com.xjbg.log.collector.annotation;

import com.xjbg.log.collector.api.LogCollector;
import com.xjbg.log.collector.api.impl.AbstractLogCollector;
import com.xjbg.log.collector.model.LogInfo;
import com.xjbg.log.collector.sensitive.SensitiveStrategy;

import java.lang.annotation.*;

/**
 * @author kesc
 * @since 2023-04-04 9:59
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface CollectorLog {

    /**
     * a key to indicate this log
     */
    String businessNo() default "";

    /**
     * the module of this log
     */
    String module() default "";

    /**
     * what has done of this log, e.g. login
     */
    String action() default "";

    /**
     * type of this log
     */
    String type() default "";

    /**
     * the method bound this log
     */
    String handleMethod() default "";

    /**
     * generated information, if present will replace params
     */
    String detail() default "";

    /**
     * log collector customized
     */
    Class<? extends LogCollector<? extends LogInfo>> collector() default AbstractLogCollector.None.class;

    /**
     * only collect log when meet error
     */
    boolean onError() default false;

    /**
     * collector log in async mode
     */
    boolean async() default true;

    /**
     * do not save response into log
     */
    boolean ignoreResponse() default false;

    /**
     * ignore a param or property in a log
     */
    @Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Inherited
    @interface Ignore {

    }

    /**
     * do sensitive strategy on a param or property
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Inherited
    @interface Sensitive {

        SensitiveStrategy strategy();

    }

}
