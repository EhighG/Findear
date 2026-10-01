package com.findear.batch.police.service;

import com.findear.batch.common.elasticsearch.IndexMappingChecks;
import com.findear.batch.police.domain.PoliceAcquiredData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 기동할 때 {@code police_acquired_data} 인덱스 매핑을 확인한다.
 * Spring Data ES는 이미 있는 인덱스의 매핑을 바꾸지 않아서(옛 동적 매핑: fdYmd date지만 atcId text 등),
 * 매핑이 다르면 WARN 한 줄만 남긴다. 자동으로 지우지 않는다. 인덱스가 아직 없으면 아무것도 하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PoliceIndexMappingChecker {

    private static final Map<String, String> EXPECTED = new LinkedHashMap<>();

    static {
        EXPECTED.put("atcId", "keyword");
        EXPECTED.put("mainPrdtClNm", "keyword");
        EXPECTED.put("fdYmd", "date");
    }

    private final ElasticsearchOperations operations;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            check();
        } catch (RuntimeException e) {
            // 매핑 확인은 참고용이라 ES 접속 문제 등으로 기동을 막지 않는다
            log.debug("police_acquired_data 매핑 확인 생략 ({})", e.getClass().getSimpleName());
        }
    }

    /** 기대와 다른 필드를 설명하는 문장 목록 (비어 있으면 일치, 인덱스가 없어도 비어 있음). 다르면 WARN을 남긴다 */
    public List<String> check() {

        List<String> problems = IndexMappingChecks.problems(operations, PoliceAcquiredData.INDEX, EXPECTED);

        if (!problems.isEmpty()) {
            log.warn("{} 인덱스 매핑이 다름 ({}) — 인덱스를 지우고 batch를 재기동하세요 (로컬은 docker compose down -v)",
                    PoliceAcquiredData.INDEX, String.join(", ", problems));
        }
        return problems;
    }
}
