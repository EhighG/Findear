package com.findear.match.dto;

/** 오류 응답. 모든 오류는 {"message": "..."} 하나의 모양이다. */
public record ErrorResponse(String message) {
}
