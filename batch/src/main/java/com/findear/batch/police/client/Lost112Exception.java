package com.findear.batch.police.client;

/**
 * Lost112 호출·응답 실패. 메시지에는 서비스 키와 키가 든 URL을 절대 넣지 않는다 (원인 예외도 연결하지 않는다:
 * RestTemplate 예외 메시지에는 요청 URL이 들어 있다).
 */
public class Lost112Exception extends RuntimeException {

    private static final int MAX_MESSAGE_LENGTH = 200;

    public Lost112Exception(String message) {
        super(truncate(message));
    }

    private static String truncate(String message) {
        if (message == null) {
            return "알 수 없는 오류";
        }
        return message.length() <= MAX_MESSAGE_LENGTH ? message : message.substring(0, MAX_MESSAGE_LENGTH) + "…";
    }
}
