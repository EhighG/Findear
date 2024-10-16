package com.findear.stubbatchserver.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FindearMatchingResDto {
    private Long findearMatchingLogId;
    private Long lostBoardId;
    private Long acquiredBoardId;
    private Float similarityRate;
    private String matchedAt;
}
