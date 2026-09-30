package com.findear.main.Alarm.push;

/**
 * 푸시 발송 요청 이벤트. 알림을 저장하는 트랜잭션이 커밋된 뒤에 발송하려고
 * NotificationService가 발행하고, PushDispatchListener가 받는다.
 */
public record PushRequestedEvent(PushMessage message) {
}
