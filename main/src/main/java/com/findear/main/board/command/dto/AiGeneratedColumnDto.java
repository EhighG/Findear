package com.findear.main.board.command.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Getter @Setter
@NoArgsConstructor
public class AiGeneratedColumnDto {
    private String category;
    private String color;
    private List<String> description;
}
