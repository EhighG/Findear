package com.findear.batch.common.exception;

/**
 * match 서버 호출이 실패했을 때(연결 불가, 시간 초과, 5xx·4xx 응답, 본문 없음). 원인 예외를 cause로 보존한다.
 * 컨트롤러 밖으로 나가면 502 고정 문구로 응답하고 원인은 로그에만 남는다 ({@link CommonControllerAdvice}).
 * 잡의 스텝에서는 분실물 하나의 실패로 집계된다.
 */
public class MatchServerException extends RuntimeException {

    public MatchServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public MatchServerException(String message) {
        super(message);
    }
}
