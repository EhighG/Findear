package com.findear.batch.common.exception;

/** 요청 입력이 잘못됐을 때(날짜 형식, page·size 범위, 필수값 누락 등). 컨트롤러 밖으로 나가면 400 {@code {status, message}}로 응답한다 ({@link CommonControllerAdvice}) */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
