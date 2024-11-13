package com.findear.main.matching.model.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class Lost112MatchingDto {
    private Long policeMatchingLogId;
    private Long lostBoardId;
    private float similarityRate;
    private LocalDate matchedAt;

    private Long acquiredBoardId;
    private String atcId;
    private String depPlace;
    private String fdFilePathImg;
    private String fdPrdtNm;
    private String fdSbjt;
    private String clrNm;
    private LocalDate fdYmd;
    private String mainPrdtClNm;

    @Builder
    public Lost112MatchingDto(Long policeMatchingLogId, Long lostBoardId, float similarityRate, LocalDate matchedAt, Long acquiredBoardId, String atcId, String depPlace, String fdFilePathImg, String fdPrdtNm, String fdSbjt, String clrNm, LocalDate fdYmd, String mainPrdtClNm) {
        this.policeMatchingLogId = policeMatchingLogId;
        this.lostBoardId = lostBoardId;
        this.similarityRate = similarityRate;
        this.matchedAt = matchedAt;
        this.acquiredBoardId = acquiredBoardId;
        this.atcId = atcId;
        this.depPlace = depPlace;
        this.fdFilePathImg = fdFilePathImg;
        this.fdPrdtNm = fdPrdtNm;
        this.fdSbjt = fdSbjt;
        this.clrNm = clrNm;
        this.fdYmd = fdYmd;
        this.mainPrdtClNm = mainPrdtClNm;
    }
}
