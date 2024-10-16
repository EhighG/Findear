package com.findear.stubmatchserver;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AIGeneratedColumDto {
    private String category;
    private String color;
    private List<String> description;
}
