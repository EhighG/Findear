package com.findear.batch.ours.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Data
public class LostBoardMatchingDto {

    private String lostBoardId;

    private String productName;

    private String color;

    private String categoryName;

    private String description;

    private String lostAt;

    private String xPos;

    private String yPos;

}
