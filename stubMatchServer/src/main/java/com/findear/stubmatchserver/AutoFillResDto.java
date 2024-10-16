package com.findear.stubmatchserver;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AutoFillResDto {
    private String message;
    private AIGeneratedColumDto result;
}
