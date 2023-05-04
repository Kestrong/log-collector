package com.xjbg.log.collector.sensitive;

import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.xjbg.log.collector.annotation.CollectorLog;

/**
 * @author kesc
 * @since 2023-04-13 11:24
 */
public class SensitiveLogAnnotationIntrospector extends NopAnnotationIntrospector {

    @Override
    public boolean hasIgnoreMarker(AnnotatedMember m) {
        return _findAnnotation(m, CollectorLog.Ignore.class) != null;
    }

    @Override
    public Object findSerializer(Annotated am) {
        CollectorLog.Sensitive sensitive = am.getAnnotation(CollectorLog.Sensitive.class);
        if (sensitive != null) {
            return SensitiveJsonSerializer.create(sensitive.strategy());
        }
        return null;
    }

}
