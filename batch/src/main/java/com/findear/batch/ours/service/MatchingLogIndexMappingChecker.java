package com.findear.batch.ours.service;

import com.findear.batch.common.elasticsearch.IndexMappingChecks;
import com.findear.batch.ours.domain.FindearMatchingLog;
import com.findear.batch.ours.domain.PoliceMatchingLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 기동할 때 매칭 로그 인덱스({@code findear_matching_log}, {@code police_matching_log}) 매핑을 확인한다.
 * 이미 있는 인덱스가 옛 매핑(R-33 이전: {@code _class}뿐이거나 동적 매핑)이면 {@code similarityRate} 정렬이 실패할 수 있어
 * {@code similarityRate}가 float이 아니거나 {@code matchingAt}이 date가 아니면 인덱스마다 WARN 한 줄만 남긴다.
 * 자동으로 지우지 않는다. 인덱스가 아직 없으면 아무것도 하지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingLogIndexMappingChecker {

    private static final Map<String, String> EXPECTED = new LinkedHashMap<>();

    static {
        EXPECTED.put("similarityRate", "float");
        EXPECTED.put("matchingAt", "date");
    }

    private final ElasticsearchOperations operations;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            check();
        } catch (RuntimeException e) {
            // 매핑 확인은 참고용이라 ES 접속 문제 등으로 기동을 막지 않는다
            log.debug("매칭 로그 매핑 확인 생략 ({})", e.getClass().getSimpleName());
        }
    }

    /** 기대와 다른 필드를 설명하는 문장 목록 (인덱스 이름 접두). 비어 있으면 일치, 인덱스가 없어도 비어 있음. 다르면 인덱스마다 WARN을 남긴다 */
    public List<String> check() {

        List<String> all = new ArrayList<>();
        for (String index : List.of(FindearMatchingLog.INDEX, PoliceMatchingLog.INDEX)) {
            List<String> problems = IndexMappingChecks.problems(operations, index, EXPECTED);
            if (!problems.isEmpty()) {
                log.warn("{} 인덱스 매핑이 다름 ({}) — 인덱스를 지우고 batch를 재기동하세요 (로컬은 docker compose down -v)",
                        index, String.join(", ", problems));
                problems.forEach(p -> all.add(index + "." + p));
            }
        }
        return all;
    }
}
