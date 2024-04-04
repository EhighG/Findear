package com.findear.main.board.query.dto;

import lombok.*;


@Getter @Setter
@NoArgsConstructor
public class PoliceAcquiredDto {

    private Long id;

    private String atcId;       // 관리ID

    private String depPlace;    // 보관장소
    private Boolean isLost;

    private String addr;        // 상세주소

    private String fdFilePathImg;       // 습득물 사진 이미지명

    private String fdPrdtNm;        // 물품명

    private String fdSbjt;      // 게시 제목

    private String clrNm;       // 색상 명

    private String fdYmd;       // 습득 일자

    private String prdtClNm;        // 물품 분류

    private String mainPrdtClNm;    // 물품 대분류

    private String subPrdtClNm;     // 물품 소분류

    public PoliceAcquiredDto(Long id, String atcId, String depPlace, String fdFilePathImg,
                              String fdPrdtNm, String fdSbjt, String clrNm, String fdYmd, String prdtClNm,
                              String mainPrdtClNm) {

        this.id = id;
        this.atcId = atcId;
        this.depPlace = depPlace;
        this.fdFilePathImg = fdFilePathImg;
        this.isLost = false;
        this.fdPrdtNm = fdPrdtNm;
        this.fdSbjt = fdSbjt;
        this.clrNm = clrNm;
        this.fdYmd = fdYmd;
        this.prdtClNm = prdtClNm;
        this.mainPrdtClNm = mainPrdtClNm;
    }

    public PoliceAcquiredDto(Long id, String atcId, String depPlace, String fdFilePathImg,
                              String fdPrdtNm, String fdSbjt, String clrNm, String fdYmd, String prdtClNm,
                              String mainPrdtClNm, String subPrdtClNm) {

        this.id = id;
        this.atcId = atcId;
        this.depPlace = depPlace;
        this.isLost = false;
        this.fdFilePathImg = fdFilePathImg;
        this.fdPrdtNm = fdPrdtNm;
        this.fdSbjt = fdSbjt;
        this.clrNm = clrNm;
        this.fdYmd = fdYmd;
        this.prdtClNm = prdtClNm;
        this.mainPrdtClNm = mainPrdtClNm;
        this.subPrdtClNm = subPrdtClNm;
    }
}

