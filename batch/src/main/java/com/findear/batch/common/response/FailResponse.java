package com.findear.batch.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 오류 응답 {@code {"status": 503, "message": "..."}} (main의 FailResponse와 같은 모양). 전체 오류 형식 정리는 R-36 */
@Getter
@AllArgsConstructor
public class FailResponse {

    private int status;
    private String message;
}
