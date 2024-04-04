package com.findear.main.board.command.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class ModelServerResponseDto {
    private String message;
    private AiGeneratedColumnDto result;
}
