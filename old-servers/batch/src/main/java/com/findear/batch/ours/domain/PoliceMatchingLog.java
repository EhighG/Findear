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
@Document(indexName = "police_matching_log")
public class PoliceMatchingLog {

    @Id
    private Long policeMatchingLogId;

    private Long lostBoardId;

    private Float similarityRate;

    private String matchingAt;

    private String acquiredBoardId;
    private String atcId;
    private String depPlace;
    private String fdFilePathImg;
    private String fdPrdtNm;
    private String fdSbjt;
    private String clrNm;
    private String fdYmd;
    private String mainPrdtClNm;

    @Builder
    public PoliceMatchingLog(Long policeMatchingLogId, Long lostBoardId,
                              Float similarityRate, String acquiredBoardId, String atcId, String depPlace,
                             String fdFilePathImg, String fdPrdtNm, String fdSbjt,
                             String clrNm, String fdYmd, String mainPrdtClNm, String matchingAt) {

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
