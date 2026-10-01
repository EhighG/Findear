package com.findear.batch.common.elasticsearch;

import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchHitsIterator;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 인덱스 문서를 엔티티가 아닌 source Map으로 읽는다.
 * 엔티티로 바꾸면 null 필드가 사라지거나 생성자 규칙이 끼어들어서, 팀 코드가 source 맵을 그대로 쓰던 곳에서만 쓴다.
 */
@Component
@RequiredArgsConstructor
public class ElasticsearchSourceReader {

    private final ElasticsearchOperations operations;

    /** 쿼리에 지정한 페이지(from·size) 하나만 읽는다. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Map<String, Object>> search(String indexName, NativeQuery query) {

        SearchHits<Map> hits = operations.search(query, Map.class, IndexCoordinates.of(indexName));

        List<Map<String, Object>> result = new ArrayList<>();
        for (SearchHit<Map> hit : hits) {
            result.add((Map<String, Object>) hit.getContent());
        }
        return result;
    }

    /** 일치하는 문서를 전부 읽는다 (scroll). 쿼리의 페이지 크기는 한 번에 가져오는 건수다. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Map<String, Object>> searchAll(String indexName, NativeQuery query) {

        List<Map<String, Object>> result = new ArrayList<>();

        try (SearchHitsIterator<Map> iterator = operations.searchForStream(query, Map.class, IndexCoordinates.of(indexName))) {
            while (iterator.hasNext()) {
                result.add((Map<String, Object>) iterator.next().getContent());
            }
        }
        return result;
    }
}
