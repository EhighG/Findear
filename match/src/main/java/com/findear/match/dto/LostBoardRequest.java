package com.findear.match.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * 분실물 (batch의 LostBoardMatchingDto). 값은 전부 문자열로 오지만 JSON 숫자도 받는다.
 * Lombok 필드 xPos/yPos는 JSON에서 xpos/ypos가 되므로 camelCase(xPos/yPos)도 별칭으로 받는다.
 */
public record LostBoardRequest(
        String lostBoardId,
        String productName,
        String color,
        String categoryName,
        String description,
        String lostAt,
        @JsonAlias("xPos") String xpos,
        @JsonAlias("yPos") String ypos) {
}
