package com.findear.stubbatchserver.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Lost112MatchingResDto {
    private String policeMatchingLogId;
    private String lostBoardId;
    private String similarityRate;
    private String matchedAt;

    private String acquiredBoardId;
    private String atcId;
    private String depPlace;
    private String fdFilePathImg;
    private String fdPrdtNm;
    private String fdSbjt;
    private String clrNm;
    private String fdYmd;
    private String mainPrdtClNm;
}
