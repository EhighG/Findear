package com.findear.batch.ours.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(indexName = "findear_matching_log")
public class FindearMatchingLog {

    @Id
    private Long findearMatchingLogId;

    private Long lostBoardId;

    private Long acquiredBoardId;

    private Float similarityRate;

    private String matchingAt;

    @Builder
    public FindearMatchingLog(Long findearMatchingLogId, Long lostBoardId,
                              Long acquiredBoardId, Float similarityRate, String matchingAt) {

        this.findearMatchingLogId = findearMatchingLogId;
        this.lostBoardId = lostBoardId;
        this.acquiredBoardId = acquiredBoardId;
        this.similarityRate = similarityRate;
        this.matchingAt = matchingAt;
    }
}
