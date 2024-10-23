package com.findear.stubbatchserver.dto;

import com.findear.stubbatchserver.domain.FindearMatching;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class FindearMatchingDto {
    private Long findearMatchingLogId;
    private Long lostBoardId;
    private Long acquiredBoardId;
    private float similarityRate;
    private String matchedAt;

    public static FindearMatchingDto of(FindearMatching findearMatching) {
        return FindearMatchingDto.builder()
                .findearMatchingLogId(findearMatching.getId())
                .lostBoardId(findearMatching.getLostBoardId())
                .acquiredBoardId(findearMatching.getAcquiredBoardId())
                .similarityRate(findearMatching.getSimilarityRate())
                .matchedAt(findearMatching.getMatchedAt().format(DateTimeFormatter.ISO_DATE))
                .build();
    }
}
