package com.findear.main.Alarm.push;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

/** {@link ConditionalOnFcm}의 판정. */
class OnFcmCondition implements Condition {

    static final String PROPERTY = "fcm.enabled";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnFcm.class.getName());
        boolean expected = (boolean) attributes.get("enabled");
        return expected == isFcmEnabled(context);
    }

    private static boolean isFcmEnabled(ConditionContext context) {
        try {
            return Boolean.TRUE.equals(context.getEnvironment().getProperty(PROPERTY, Boolean.class));
        } catch (ConversionException e) {
            throw new IllegalStateException("fcm.enabled(FCM_ENABLED) 값을 true/false로 읽을 수 없습니다: '"
                    + context.getEnvironment().getProperty(PROPERTY)
                    + "'. true 또는 false로 설정하세요.", e);
        }
    }
}
