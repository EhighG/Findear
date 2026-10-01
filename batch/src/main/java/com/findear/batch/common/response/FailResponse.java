package com.findear.batch.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 오류 응답 {@code {"status": 503, "message": "..."}} (main의 FailResponse와 같은 모양). status는 실제 HTTP 상태와 같다 (D-51, CommonControllerAdvice) */
@Getter
@AllArgsConstructor
public class FailResponse {

    private int status;
    private String message;
}
