package com.findear.batch.police.client;

import java.util.List;
import java.util.Map;

/**
 * 응답 한 페이지를 파싱한 결과.
 *
 * @param totalCount 응답에 totalCount가 없으면 null
 * @param items      item 하나당 {태그 이름: 앞뒤 공백을 뺀 텍스트}
 */
public record Lost112Page(Integer pageNo, Integer numOfRows, Long totalCount, List<Map<String, String>> items) {

    public static Lost112Page empty() {
        return new Lost112Page(null, null, 0L, List.of());
    }
}
