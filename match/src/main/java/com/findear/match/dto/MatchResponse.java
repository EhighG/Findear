package com.findear.match.dto;

import java.util.List;

/** 매칭 응답 {"message": str, "result": [...] 또는 null}. null도 키가 그대로 나간다. */
public record MatchResponse<T>(String message, List<T> result) {
}
