package com.findear.batch.ours.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.findear.batch.ours.domain.FindearMatchingLog;
import com.findear.batch.ours.domain.MatchingLogFormat;
import com.findear.batch.ours.domain.PoliceMatchingLog;
import com.findear.batch.ours.repository.FindearMatchingLogRepository;
import com.findear.batch.ours.repository.PoliceMatchingLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.types.ConflictsType;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 매칭 로그(findear_matching_log, police_matching_log)를 쓰는 유일한 곳 (06 §3).
 * 분실물 하나의 매칭이 성공적으로 끝나면 그 분실물의 해당 종류 로그가 이번 결과와 정확히 같아진다.
 * <ol>
 *   <li>문서 ID는 결정적이다: findear {@code {lostBoardId}-{acquiredBoardId}}, police {@code {lostBoardId}-{atcId}}. 같은 쌍은 덮어쓴다(점수·시각 갱신).</li>
 *   <li>새 결과를 먼저 upsert하고, 같은 {@code lostBoardId}의 로그 중 이번 결과에 없는 것을 뒤에 지운다 (읽는 쪽이 빈 목록을 보는 순간이 없다).</li>
 *   <li>결과가 null이거나 빈 목록(후보 없음)이면 그 분실물의 해당 로그를 모두 지운다.</li>
 *   <li>결과 항목이 1건 이상인데 유효 항목이 0건이면 잘못된 응답으로 보고 upsert도 삭제도 하지 않는다(기존 로그 유지, WARN 한 줄, 0 반환).</li>
 *   <li>끝나면 refresh해서 바로 조회에 보이게 한다.</li>
 * </ol>
 * match 호출이 실패한 경우에는 이 클래스를 부르지 않으므로 기존 로그가 그대로 남는다 (호출하는 쪽 책임).
 * match 결과 항목은 null에 안전하게 변환한다: 값이 없으면 null로 저장하고,
 * ID·점수에 필요한 값이 없거나 숫자가 아닌 항목만 WARN 한 줄과 함께 건너뛴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingLogWriter {

    private final ElasticsearchOperations operations;
    private final FindearMatchingLogRepository findearMatchingLogRepository;
    private final PoliceMatchingLogRepository policeMatchingLogRepository;

    /**
     * @param results match {@code /matching/findear}의 {@code result}. null이면 후보 없음 (그 분실물의 로그를 모두 지운다)
     * @return 저장한 로그 수
     */
    public int replaceFindearLogs(long lostBoardId, List<Map<String, Object>> results) {

        LocalDateTime now = MatchingLogFormat.now();
        Map<String, FindearMatchingLog> logs = new LinkedHashMap<>();

        if (results != null) {
            for (Map<String, Object> res : results) {
                FindearMatchingLog matchingLog = toFindearLog(lostBoardId, res, now);
                if (matchingLog != null) {
                    logs.put(matchingLog.getFindearMatchingLogId(), matchingLog);
                }
            }
        }

        if (!replace(FindearMatchingLog.class, FindearMatchingLog.INDEX, findearMatchingLogRepository, lostBoardId, logs,
                results == null ? 0 : results.size(), "findear")) {
            return 0;
        }
        log.info("findear 로그 저장 완료 (lostBoardId={}, {}건)", lostBoardId, logs.size());
        return logs.size();
    }

    /**
     * @param results match {@code /matching/lost}의 {@code result}. null이면 후보 없음 (그 분실물의 로그를 모두 지운다)
     * @return 저장한 로그 수
     */
    public int replacePoliceLogs(long lostBoardId, List<Map<String, Object>> results) {

        LocalDateTime now = MatchingLogFormat.now();
        Map<String, PoliceMatchingLog> logs = new LinkedHashMap<>();

        if (results != null) {
            for (Map<String, Object> res : results) {
                PoliceMatchingLog matchingLog = toPoliceLog(lostBoardId, res, now);
                if (matchingLog != null) {
                    logs.put(matchingLog.getPoliceMatchingLogId(), matchingLog);
                }
            }
        }

        if (!replace(PoliceMatchingLog.class, PoliceMatchingLog.INDEX, policeMatchingLogRepository, lostBoardId, logs,
                results == null ? 0 : results.size(), "lost112")) {
            return 0;
        }
        log.info("lost112 로그 저장 완료 (lostBoardId={}, {}건)", lostBoardId, logs.size());
        return logs.size();
    }

    /**
     * @param received match가 돌려준 항목 수 (null이면 0)
     * @return 로그를 교체했으면 true. 항목이 1건 이상 왔는데 유효 항목이 0건이면 잘못된 응답으로 보고 기존 로그를 그대로 둔 채 false
     */
    private <T> boolean replace(Class<T> type, String index, ElasticsearchRepository<T, String> repository,
                                long lostBoardId, Map<String, T> logs, int received, String kind) {

        // 항목은 왔는데 하나도 못 쓰면 응답이 잘못된 것이다. 정상 로그가 사라지지 않게 upsert도 삭제도 하지 않는다
        // (results가 null이거나 빈 목록이면 match가 "후보 없음"이라고 답한 것이라 아래에서 그 분실물의 로그를 모두 지운다)
        if (received > 0 && logs.isEmpty()) {
            log.warn("{} 매칭 결과 {}건이 모두 유효하지 않아 건너뜀, 기존 로그는 그대로 둠 (lostBoardId={})", kind, received, lostBoardId);
            return false;
        }

        // 1) 새 결과를 먼저 upsert
        if (!logs.isEmpty()) {
            repository.saveAll(logs.values());
        }

        // 2) 같은 분실물의 로그 중 이번 결과에 없는 것을 지운다. 결과가 비었으면 그 분실물의 로그 전부
        List<String> keepIds = new ArrayList<>(logs.keySet());
        NativeQuery staleQuery = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(b -> {
                    b.filter(f -> f.term(t -> t.field("lostBoardId").value(lostBoardId)));
                    if (!keepIds.isEmpty()) {
                        b.mustNot(m -> m.ids(i -> i.values(keepIds)));
                    }
                    return b;
                })))
                .build();
        operations.delete(DeleteQuery.builder(staleQuery).withConflicts(ConflictsType.Proceed).withRefresh(true).build(),
                type, IndexCoordinates.of(index));

        // 3) 바로 조회에 보이게
        operations.indexOps(type).refresh();
        return true;
    }

    private FindearMatchingLog toFindearLog(long lostBoardId, Map<String, Object> res, LocalDateTime now) {

        if (res == null) {
            log.warn("findear 매칭 결과 항목이 비어 있어 건너뜀 (lostBoardId={})", lostBoardId);
            return null;
        }

        Long itemLostBoardId = toLong(res.get("lostBoardId"));
        Long acquiredBoardId = toLong(res.get("acquiredBoardId"));
        Float similarityRate = toFloat(res.get("similarityRate"));
        if (itemLostBoardId == null || acquiredBoardId == null || similarityRate == null) {
            log.warn("findear 매칭 결과 항목을 건너뜀 (lostBoardId·acquiredBoardId·similarityRate 중 없거나 숫자가 아님): {}", res);
            return null;
        }
        if (itemLostBoardId != lostBoardId) {
            log.warn("findear 매칭 결과 항목의 lostBoardId({})가 요청한 분실물({})과 달라 건너뜀", itemLostBoardId, lostBoardId);
            return null;
        }

        return FindearMatchingLog.builder()
                .findearMatchingLogId(lostBoardId + "-" + acquiredBoardId)
                .lostBoardId(lostBoardId)
                .acquiredBoardId(acquiredBoardId)
                .similarityRate(similarityRate)
                .matchingAt(now)
                .build();
    }

    private PoliceMatchingLog toPoliceLog(long lostBoardId, Map<String, Object> res, LocalDateTime now) {

        if (res == null) {
            log.warn("lost112 매칭 결과 항목이 비어 있어 건너뜀 (lostBoardId={})", lostBoardId);
            return null;
        }

        Long itemLostBoardId = toLong(res.get("lostBoardId"));
        String atcId = toText(res.get("atcId"));
        Float similarityRate = toFloat(res.get("similarityRate"));
        if (itemLostBoardId == null || atcId == null || atcId.isBlank() || similarityRate == null) {
            log.warn("lost112 매칭 결과 항목을 건너뜀 (lostBoardId·atcId·similarityRate 중 없거나 숫자가 아님): {}", res);
            return null;
        }
        if (itemLostBoardId != lostBoardId) {
            log.warn("lost112 매칭 결과 항목의 lostBoardId({})가 요청한 분실물({})과 달라 건너뜀", itemLostBoardId, lostBoardId);
            return null;
        }

        return PoliceMatchingLog.builder()
                .policeMatchingLogId(lostBoardId + "-" + atcId)
                .lostBoardId(lostBoardId)
                .similarityRate(similarityRate)
                .matchingAt(now)
                .acquiredBoardId(toText(res.get("acquiredBoardId")))
                .atcId(atcId)
                .depPlace(toText(res.get("depPlace")))
                .fdFilePathImg(toText(res.get("fdFilePathImg")))
                .fdPrdtNm(toText(res.get("fdPrdtNm")))
                .fdSbjt(toText(res.get("fdSbjt")))
                .clrNm(toText(res.get("clrNm")))
                .fdYmd(toDate(res.get("fdYmd")))
                .mainPrdtClNm(toText(res.get("mainPrdtClNm")))
                .build();
    }

    private static String toText(Object value) {
        return value == null ? null : value.toString();
    }

    /** 정수로 읽을 수 있을 때만 (1, "1", 1.0). 아니면 null */
    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.toString().trim()).longValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }

    /** 유한한 숫자일 때만. 아니면 null */
    private static Float toFloat(Object value) {
        if (value == null) {
            return null;
        }
        try {
            float parsed = Float.parseFloat(value.toString().trim());
            return Float.isFinite(parsed) ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** yyyy-MM-dd로 읽을 수 있으면 그 날짜, 값이 없거나 읽을 수 없으면 null (항목은 건너뛰지 않는다) */
    private static LocalDate toDate(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        try {
            return LocalDate.parse(text.length() > 10 ? text.substring(0, 10) : text);
        } catch (DateTimeParseException e) {
            log.warn("fdYmd를 날짜로 읽을 수 없어 null로 저장: {}", text);
            return null;
        }
    }
}
