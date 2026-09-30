package com.findear.main.Alarm.push;

/**
 * 웹푸시 한 건. token은 FCM 등록 토큰(tbl_notification.token)이고 로그에 남기지 않는다.
 */
public record PushMessage(Long memberId, String token, String title, String body) {

    // 객체째 로그에 남거나 Spring 오류 메시지에 인자로 찍혀도 토큰이 새지 않게 가린다
    @Override
    public String toString() {
        return "PushMessage[memberId=" + memberId + ", token=***, title=" + title + ", body=" + body + "]";
    }
}
