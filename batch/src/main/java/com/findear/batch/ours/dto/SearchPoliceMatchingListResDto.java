package com.findear.batch.ours.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SearchPoliceMatchingListResDto {

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
