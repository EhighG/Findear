package com.findear.batch.ours.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@AllArgsConstructor
@Data
public class PoliceAcquiredBoardMatchingDto {

    private String id;

    private String atcId;

    private String depPlace;

    private String fdFilePathImg;

    private String fdPrdtNm;

    private String fdSbjt;

    private String clrNm;

    private String fdYmd;

    private String mainPrdtClNm;

}
