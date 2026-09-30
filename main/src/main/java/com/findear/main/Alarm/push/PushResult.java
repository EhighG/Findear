package com.findear.main.Alarm.push;

/**
 * 푸시 발송 결과. 발송 실패는 예외로 던지지 않고 결과로 돌려준다 (알림을 일으킨 흐름을 깨지 않기 위해).
 */
public enum PushResult {
    /** 발송 요청이 FCM에 접수됨 */
    SENT,
    /** 발송하지 않음 (fcm.enabled=false) */
    SKIPPED,
    /** 토큰이 더 이상 유효하지 않음 (FCM UNREGISTERED). 호출한 쪽에서 토큰을 지운다 */
    TOKEN_INVALID,
    /** 그 밖의 발송 실패. 토큰은 유지한다 */
    FAILED
}
