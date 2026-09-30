package com.findear.match.dto;

/** Lost112 습득물 후보 (batch의 PoliceAcquiredBoardMatchingDto). */
public record LostAcquiredRequest(
        String id,
        String atcId,
        String depPlace,
        String fdFilePathImg,
        String fdPrdtNm,
        String fdSbjt,
        String clrNm,
        String fdYmd,
        String mainPrdtClNm) {
}
