package com.findear.match.dto;

import java.util.List;

/** POST /process 응답 {"message":"success","result":{category,color,description[5]}}. */
public record ProcessResponse(String message, Result result) {

    public record Result(String category, String color, List<String> description) {
    }
}
