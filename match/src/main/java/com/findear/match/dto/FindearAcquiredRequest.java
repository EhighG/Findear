package com.findear.match.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/** Findear 습득물 후보 (batch의 AcquiredBoardMatchingDto). */
public record FindearAcquiredRequest(
        String acquiredBoardId,
        String productName,
        String color,
        String categoryName,
        String description,
        @JsonAlias("xPos") String xpos,
        @JsonAlias("yPos") String ypos,
        String registeredAt) {
}
