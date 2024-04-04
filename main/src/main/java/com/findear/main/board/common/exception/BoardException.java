package com.findear.main.board.common.exception;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class BoardException extends RuntimeException {

    public BoardException(String str) {
        super(str);
    }
}
