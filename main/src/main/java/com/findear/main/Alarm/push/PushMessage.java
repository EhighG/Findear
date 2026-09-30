package com.findear.main.Alarm.push;

/**
 * 웹푸시 한 건. token은 FCM 등록 토큰(tbl_notification.token)이고 로그에 남기지 않는다.
 */
public record PushMessage(Long memberId, String token, String title, String body) {
}
