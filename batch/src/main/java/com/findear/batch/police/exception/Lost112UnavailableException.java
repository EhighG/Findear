package com.findear.batch.police.exception;

/**
 * Lost112 호출이 모두 실패해 하나도 수집하지 못했을 때. 수동 실행 API는 502로 응답한다 (D-49).
 * 메시지에는 실패 원인 코드만 있고 서비스 키·키가 든 URL은 없다.
 */
public class Lost112UnavailableException extends RuntimeException {

    public Lost112UnavailableException(String detail) {
        super("Lost112 호출에 실패했습니다: " + detail);
    }
}
