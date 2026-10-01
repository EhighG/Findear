package com.findear.batch.ours.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

/**
 * Findear 습득물과의 매칭 로그 (06 §3). 문서 ID는 {@code {lostBoardId}-{acquiredBoardId}}라 같은 쌍을 다시 매칭하면 덮어쓴다.
 * 매핑은 아래 어노테이션이 정하며 Spring Data ES가 인덱스를 만들 때만 적용된다 (이미 있는 인덱스는 바꾸지 않는다, MatchingLogIndexMappingChecker 참고).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(indexName = FindearMatchingLog.INDEX)
public class FindearMatchingLog {

    public static final String INDEX = "findear_matching_log";

    @Id
    @Field(type = FieldType.Keyword)
    private String findearMatchingLogId;    // 문서 ID = {lostBoardId}-{acquiredBoardId}

    @Field(type = FieldType.Long)
    private Long lostBoardId;

    @Field(type = FieldType.Long)
    private Long acquiredBoardId;

    @Field(type = FieldType.Float)
    private Float similarityRate;

    @Field(type = FieldType.Date, format = {}, pattern = MatchingLogFormat.PATTERN)
    private LocalDateTime matchingAt;       // Asia/Seoul, 초 단위

    @Builder
    public FindearMatchingLog(String findearMatchingLogId, Long lostBoardId,
                              Long acquiredBoardId, Float similarityRate, LocalDateTime matchingAt) {

        this.findearMatchingLogId = findearMatchingLogId;
        this.lostBoardId = lostBoardId;
        this.acquiredBoardId = acquiredBoardId;
        this.similarityRate = similarityRate;
        this.matchingAt = matchingAt;
    }
}
