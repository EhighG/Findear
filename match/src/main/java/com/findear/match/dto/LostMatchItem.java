package com.findear.match.dto;

/**
 * /matching/lost 결과 항목. 입력 필드를 그대로 되돌려준다.
 * acquiredBoardId는 id가 정수면 JSON 숫자(Long), 아니면 원래 문자열, 없으면 null.
 */
public record LostMatchItem(
        long lostBoardId,
        Object acquiredBoardId,
        double similarityRate,
        String atcId,
        String depPlace,
        String fdFilePathImg,
        String fdPrdtNm,
        String fdSbjt,
        String clrNm,
        String fdYmd,
        String mainPrdtClNm) {
}
