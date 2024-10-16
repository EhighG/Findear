package com.findear.stubbatchserver.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Lost112BoardListResDto {
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
