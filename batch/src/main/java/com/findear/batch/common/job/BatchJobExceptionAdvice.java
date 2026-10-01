package com.findear.batch.common.job;

import com.findear.batch.common.response.FailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 수동 실행 API에서 같은 잡이 이미 돌고 있으면 409 {@code {status, message}}. 나머지 오류는 {@code CommonControllerAdvice}가 처리하므로 이 advice가 먼저 오도록 HIGHEST_PRECEDENCE를 쓴다 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class BatchJobExceptionAdvice {

    @ExceptionHandler(BatchJobBusyException.class)
    public ResponseEntity<FailResponse> handleBusy(BatchJobBusyException e) {
        log.warn(e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new FailResponse(HttpStatus.CONFLICT.value(), e.getMessage()));
    }
}
