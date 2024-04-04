package com.findear.main.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FailResponse {

    int status;
    String message;

    public FailResponse(int status, String message){
        this.status = status;
        this.message = message;
    }
}
