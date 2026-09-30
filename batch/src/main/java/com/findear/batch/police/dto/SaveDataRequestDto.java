package com.findear.batch.police.dto;

import lombok.Data;

@Data
public class SaveDataRequestDto {

    private String atcId;       // 관리ID

    private String depPlace;    // 보관장소

    private String addr;        // 상세주소

    private String fdFilePathImg;       // 습득물 사진 이미지명

    private String fdPrdtNm;        // 물품명

    private String fdSbjt;      // 게시 제목

    private String clrNm;       // 색상 명

    private String fdSn;        // 습득 순번

    private String fdYmd;       // 습득 일자

    private String prdtClNm;        // 물품 대분류

    private String subPrdtClNm;     // 물품 소분류

    private String rnum;        // 일련번호
}
