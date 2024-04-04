package com.findear.main.Alarm.common.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AlarmException extends RuntimeException{

    public AlarmException(String msg) {
        super(msg);
    }
}
