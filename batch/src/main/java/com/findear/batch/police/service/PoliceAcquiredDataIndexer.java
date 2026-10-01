package com.findear.batch.police.service;

import com.findear.batch.police.domain.PoliceAcquiredData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 문서 목록을 {@code police_acquired_data}에 bulk로 넣는다. 문서 ID는 문서의 id(= atcId)라 같은 atcId는 덮어쓴다 (upsert).
 * 항목 하나라도 실패하면 BulkFailureException을 던진다 (Spring Data ES 기본 동작).
 */
@Component
@RequiredArgsConstructor
public class PoliceAcquiredDataIndexer {

    private final ElasticsearchOperations operations;

    public void index(List<PoliceAcquiredData> documents) {

        if (documents.isEmpty()) {
            return;
        }
        List<IndexQuery> queries = documents.stream()
                .map(document -> new IndexQueryBuilder().withId(document.getId()).withObject(document).build())
                .toList();
        operations.bulkIndex(queries, IndexCoordinates.of(PoliceAcquiredData.INDEX));
    }

    /** 방금 넣은 문서를 검색(count·search)에서 보이게 한다. 수집이 끝난 뒤 한 번만 부른다 */
    public void refresh() {
        operations.indexOps(IndexCoordinates.of(PoliceAcquiredData.INDEX)).refresh();
    }
}
