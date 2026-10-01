package com.findear.batch.police.exception;

import com.findear.batch.common.response.FailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Lost112 수집 예외를 HTTP 상태로 옮긴다 (D-49: 키 없음 503, 외부 실패 502). main의 ExternalServiceExceptionAdvice와 같은 규칙.
 * batch의 전체 오류 응답 형식 정리는 R-36이라 지금은 이 두 예외만 처리한다.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class Lost112ExceptionAdvice {

    @ExceptionHandler(Lost112NotConfiguredException.class)
    public ResponseEntity<FailResponse> handleNotConfigured(Lost112NotConfiguredException e) {
        log.warn(e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new FailResponse(HttpStatus.SERVICE_UNAVAILABLE.value(), e.getMessage()));
    }

    @ExceptionHandler(Lost112UnavailableException.class)
    public ResponseEntity<FailResponse> handleUnavailable(Lost112UnavailableException e) {
        log.warn(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new FailResponse(HttpStatus.BAD_GATEWAY.value(), e.getMessage()));
    }
}
