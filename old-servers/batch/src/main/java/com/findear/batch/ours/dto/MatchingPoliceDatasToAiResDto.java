package com.findear.batch.ours.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MatchingPoliceDatasToAiResDto {

    private Object lostBoardId;

    private Object acquiredBoardId;

    private Object similarityRate;

    private Object atcId;

    private Object depPlace;

    private Object fdFilePathImg;

    private Object fdPrdtNm;

    private Object fdSbjt;

    private Object clrNm;

    private Object fdYmd;

    private Object mainPrdtClNm;

}
