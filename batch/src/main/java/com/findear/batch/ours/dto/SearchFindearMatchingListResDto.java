package com.findear.batch.ours.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
public class SearchFindearMatchingListResDto {

    private Long findearMatchingLogId;

    private Long lostBoardId;

    private Long acquiredBoardId;

    private Float similarityRate;

    private String matchedAt;

    public SearchFindearMatchingListResDto(Long findearMatchingLogId, Long lostBoardId,
                                           Long acquiredBoardId, Float similarityRate, String matchedAt) {
        this.findearMatchingLogId = findearMatchingLogId;
        this.lostBoardId = lostBoardId;
        this.acquiredBoardId = acquiredBoardId;
        this.similarityRate = similarityRate;
        this.matchedAt = matchedAt;
    }
}
