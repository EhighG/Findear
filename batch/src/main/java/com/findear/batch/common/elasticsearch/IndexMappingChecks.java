package com.findear.batch.common.elasticsearch;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 이미 있는 인덱스의 필드 타입이 기대와 같은지 확인한다. Spring Data ES는 이미 있는 인덱스의 매핑을 바꾸지 않아서
 * (옛 동적 매핑이 남아 있을 수 있다) 기동할 때 이 확인으로 WARN만 남기고, 자동으로 지우지 않는다.
 */
public final class IndexMappingChecks {

    private IndexMappingChecks() {
    }

    /**
     * @param expectedTypes 필드 이름 → 기대하는 ES 타입 (예: {@code similarityRate} → {@code float})
     * @return 기대와 다른 필드를 설명하는 문장 목록. 비어 있으면 일치하고, 인덱스가 없어도 비어 있다
     */
    public static List<String> problems(ElasticsearchOperations operations, String index, Map<String, String> expectedTypes) {

        IndexOperations indexOps = operations.indexOps(IndexCoordinates.of(index));
        if (!indexOps.exists()) {
            return List.of();
        }

        Object properties = indexOps.getMapping().get("properties");
        List<String> problems = new ArrayList<>();
        expectedTypes.forEach((field, expected) -> {
            Object actual = null;
            if (properties instanceof Map<?, ?> map && map.get(field) instanceof Map<?, ?> fieldMapping) {
                actual = fieldMapping.get("type");
            }
            if (!expected.equals(actual)) {
                problems.add(field + ": " + (actual == null ? "매핑 없음" : actual) + " (기대 " + expected + ")");
            }
        });
        return problems;
    }
}
