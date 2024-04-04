package com.findear.main.matching.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class BatchServerMatchingResDto {
    private Long id;
    private Long lostBoardId;
    private Long acquiredBoardId;
    private Float similarityRate;
    private LocalDateTime matchedAt;

    public BatchServerMatchingResDto(Long findearMatchingLogId, Long lostBoardId, Long acquiredBoardId,
                                     Float similarityRate, String matchingAt) {
        this.id = findearMatchingLogId;
        this.lostBoardId = lostBoardId;
        this.acquiredBoardId = acquiredBoardId;
        this.similarityRate = similarityRate;
        this.matchedAt = LocalDateTime.parse(matchingAt);
    }
}
