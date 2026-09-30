package com.findear.batch.ours.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class SearchScrapBoardResDto {

    private String id;

    private String atcId;

    private String depPlace;

    private String fdFilePathImg;

    private String fdPrdtNm;

    private String fdSbjt;

    private String clrNm;

    private String fdYmd;

    private String prdtClNm;

    private String mainPrdtClNm;

    private String subPrdtClNm;

}
