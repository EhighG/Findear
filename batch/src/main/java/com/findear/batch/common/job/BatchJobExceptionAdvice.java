package com.findear.batch.common.job;

import com.findear.batch.common.response.FailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 수동 실행 API에서 같은 잡이 이미 돌고 있으면 409 {@code {status, message}}. batch의 전체 오류 형식 정리는 R-36 */
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
