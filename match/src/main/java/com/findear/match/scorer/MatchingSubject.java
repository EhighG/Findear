package com.findear.match.scorer;

/**
 * 점수 계산에 쓰는 분실물·후보 습득물의 공통 모양. Findear 습득물과 Lost112 습득물을 같은 형태로 담는다.
 * 값은 요청에 온 그대로이며 null일 수 있다.
 *
 * <pre>
 * 분실물          key = lostBoardId(정수 문자열)  category = categoryName  color = color  productName  description
 * Findear 습득물  key = acquiredBoardId(정수 문자열)  category = categoryName  color = color  productName  description
 * Lost112 습득물  key = atcId(비어 있으면 id)  category = mainPrdtClNm  color = clrNm  productName = fdPrdtNm  description = fdSbjt
 * </pre>
 *
 * @param key         식별자. 결정적 난수의 입력으로 쓴다
 * @param category    카테고리 이름
 * @param color       색상 이름
 * @param productName 물품명
 * @param description 설명
 */
public record MatchingSubject(
        String key,
        String category,
        String color,
        String productName,
        String description) {
}
