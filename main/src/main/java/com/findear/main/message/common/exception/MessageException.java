package com.findear.main.message.common.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class MessageException extends RuntimeException{

    public MessageException(String msg) {
        super(msg);
    }
}
