package com.findear.main.common.exception;

import com.findear.main.common.response.FailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 외부 연동 예외를 HTTP 상태로 옮긴다 (D-49).
 * 다른 @RestControllerAdvice(CommonControllerAdvice, MemberControllerAdvice)는 Exception 전체를 잡는 핸들러가 있어서,
 * 가장 높은 우선순위로 두어 이 두 예외가 항상 여기서 처리되게 한다.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ExternalServiceExceptionAdvice {

    @ExceptionHandler(ExternalServiceNotConfiguredException.class)
    public ResponseEntity<FailResponse> handleNotConfigured(ExternalServiceNotConfiguredException e) {
        log.warn(e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new FailResponse(HttpStatus.SERVICE_UNAVAILABLE.value(), e.getMessage()));
    }

    @ExceptionHandler(ExternalServiceUnavailableException.class)
    public ResponseEntity<FailResponse> handleUnavailable(ExternalServiceUnavailableException e) {
        Throwable cause = e.getCause();
        log.warn("{} (원인: {})", e.getMessage(), cause == null ? "-" : cause.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new FailResponse(HttpStatus.BAD_GATEWAY.value(), e.getMessage()));
    }
}
