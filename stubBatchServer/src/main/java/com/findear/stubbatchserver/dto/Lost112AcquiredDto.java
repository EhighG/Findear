package com.findear.stubbatchserver.dto;

import com.findear.stubbatchserver.domain.Lost112Acquired;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class Lost112AcquiredDto {
    private Long id;
    private String atcId;
    private String depPlace;
    private String fdFilePathImg;
    private String fdPrdtNm;
    private String fdSbjt;
    private String clrNm;
    private LocalDate fdYmd;
    private String prdtClNm;
    private String mainPrdtClNm;
    private String subPrdtClNm;

    public static Lost112AcquiredDto of(Lost112Acquired entity) {
        return Lost112AcquiredDto.builder()
                .id(entity.getId())
                .atcId(entity.getAtcId())
                .depPlace(entity.getDepPlace())
                .fdFilePathImg(entity.getFdFilePathImg())
                .fdPrdtNm(entity.getFdPrdtNm())
                .fdSbjt(entity.getFdSbjt())
                .clrNm(entity.getClrNm())
                .fdYmd(entity.getFdYmd())
                .prdtClNm(entity.getPrdtClNm())
                .mainPrdtClNm(entity.getMainPrdtClNm())
                .subPrdtClNm(entity.getSubPrdtClNm())
                .build();
    }
}
