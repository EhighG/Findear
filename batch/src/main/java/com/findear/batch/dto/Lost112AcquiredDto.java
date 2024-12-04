package com.findear.batch.dto;

import com.findear.batch.domain.Lost112Acquired;
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

    public static Lost112AcquiredDto of(Lost112Acquired lost112Acquired) {
        return Lost112AcquiredDto.builder()
                .id(lost112Acquired.getId())
                .atcId(lost112Acquired.getAtcId())
                .depPlace(lost112Acquired.getDepPlace())
                .fdFilePathImg(lost112Acquired.getFdFilePathImg())
                .fdPrdtNm(lost112Acquired.getFdPrdtNm())
                .fdSbjt(lost112Acquired.getFdSbjt())
                .clrNm(lost112Acquired.getClrNm())
                .fdYmd(lost112Acquired.getFdYmd())
                .prdtClNm(lost112Acquired.getPrdtClNm())
                .mainPrdtClNm(lost112Acquired.getMainPrdtClNm())
                .subPrdtClNm(lost112Acquired.getSubPrdtClNm())
                .build();
    }
}
