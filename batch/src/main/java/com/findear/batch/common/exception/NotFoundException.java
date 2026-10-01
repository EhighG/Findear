package com.findear.batch.common.exception;

/** 찾는 대상(분실물 등)이 없을 때. 컨트롤러 밖으로 나가면 404 {@code {status, message}}로 응답한다 ({@link CommonControllerAdvice}) */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
