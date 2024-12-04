package com.findear.batch.police.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class PoliceException extends RuntimeException{

    public PoliceException(String msg) {
        super(msg);
    }
}
