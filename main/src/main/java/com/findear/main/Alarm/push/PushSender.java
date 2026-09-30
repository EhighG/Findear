package com.findear.main.Alarm.push;

/**
 * 웹푸시 발송 추상화. fcm.enabled=true면 {@link FcmPushSender}, 아니면(기본) {@link NoopPushSender}.
 * 구현은 예외를 밖으로 던지지 않는다.
 */
public interface PushSender {

    PushResult send(PushMessage message);
}
