package com.findear.batch.common.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class FindearException extends RuntimeException {

    public FindearException(String msg) {
        super(msg);
    }
}
