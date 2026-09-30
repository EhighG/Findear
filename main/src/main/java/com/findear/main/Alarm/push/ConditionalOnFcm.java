package com.findear.main.Alarm.push;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * fcm.enabled 값으로 빈을 켜고 끈다. 값은 스프링이 boolean으로 읽는 표기(true/false, yes/no, on/off, 1/0)를 모두 받고,
 * 없거나 비어 있으면 false다. boolean으로 읽을 수 없는 값이면 fcm.enabled(FCM_ENABLED) 문제라는 메시지로 기동을 멈춘다.
 *
 * Spring Boot 3.5의 @ConditionalOnBooleanProperty는 문자열 "true"/"false"만 비교해서 yes·1 같은 값이면
 * 켜는 쪽과 끄는 쪽이 모두 꺼져 PushSender 빈이 0개가 되므로 쓰지 않는다.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Conditional(OnFcmCondition.class)
public @interface ConditionalOnFcm {

    /** true면 fcm.enabled가 켜져 있을 때, false면 꺼져 있을 때(기본) 빈을 만든다. */
    boolean enabled();
}
