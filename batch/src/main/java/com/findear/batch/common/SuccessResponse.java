package com.findear.batch.common;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class SuccessResponse {

    int status;
    String message;
    Object result;

    public SuccessResponse(int status, String message){
        this.status = status;
        this.message = message;
    }
}
