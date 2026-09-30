package com.findear.match.service;

/** 요청 값이 잘못됐을 때 (400). 메시지는 그대로 응답의 message가 된다. */
public class BadRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BadRequestException(String message) {
        super(message);
    }
}
