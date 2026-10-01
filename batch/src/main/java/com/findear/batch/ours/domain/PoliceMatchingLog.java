package com.findear.batch.ours.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lost112 습득물과의 매칭 로그 (06 §3). 문서 ID는 {@code {lostBoardId}-{atcId}}라 같은 쌍을 다시 매칭하면 덮어쓴다.
 * 습득물 필드는 매칭 시점의 사본이다. 매핑은 아래 어노테이션이 정하며 인덱스를 만들 때만 적용된다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(indexName = PoliceMatchingLog.INDEX)
public class PoliceMatchingLog {

    public static final String INDEX = "police_matching_log";

    @Id
    @Field(type = FieldType.Keyword)
    private String policeMatchingLogId;     // 문서 ID = {lostBoardId}-{atcId}

    @Field(type = FieldType.Long)
    private Long lostBoardId;

    @Field(type = FieldType.Float)
    private Float similarityRate;

    @Field(type = FieldType.Date, format = {}, pattern = MatchingLogFormat.PATTERN)
    private LocalDateTime matchingAt;       // Asia/Seoul, 초 단위

    @Field(type = FieldType.Keyword)
    private String acquiredBoardId;

    @Field(type = FieldType.Keyword)
    private String atcId;

    @Field(type = FieldType.Keyword)
    private String depPlace;

    @Field(type = FieldType.Keyword, index = false)
    private String fdFilePathImg;

    @Field(type = FieldType.Keyword)
    private String fdPrdtNm;

    @Field(type = FieldType.Text)
    private String fdSbjt;

    @Field(type = FieldType.Keyword)
    private String clrNm;

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd")
    private LocalDate fdYmd;                // 인덱스·JSON 모두 yyyy-MM-dd

    @Field(type = FieldType.Keyword)
    private String mainPrdtClNm;

    @Builder
    public PoliceMatchingLog(String policeMatchingLogId, Long lostBoardId,
                             Float similarityRate, String acquiredBoardId, String atcId, String depPlace,
                             String fdFilePathImg, String fdPrdtNm, String fdSbjt,
                             String clrNm, LocalDate fdYmd, String mainPrdtClNm, LocalDateTime matchingAt) {

        this.policeMatchingLogId = policeMatchingLogId;
        this.lostBoardId = lostBoardId;
        this.similarityRate = similarityRate;
        this.acquiredBoardId = acquiredBoardId;
        this.atcId = atcId;
        this.depPlace = depPlace;
        this.fdFilePathImg = fdFilePathImg;
        this.fdPrdtNm = fdPrdtNm;
        this.fdSbjt = fdSbjt;
        this.clrNm = clrNm;
        this.fdYmd = fdYmd;
        this.mainPrdtClNm = mainPrdtClNm;
        this.matchingAt = matchingAt;
    }
}
