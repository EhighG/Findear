package com.findear.batch.ours.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findear.batch.common.exception.MatchServerException;
import com.findear.batch.ours.domain.FindearMatchingLog;
import com.findear.batch.ours.domain.PoliceMatchingLog;
import com.findear.batch.ours.dto.LostBoardMatchingDto;
import com.findear.batch.ours.dto.MatchingAllDatasToAiResDto;
import com.findear.batch.support.IntegrationTestBase;
import com.findear.batch.support.MatchMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 매칭 로그 쓰기 규칙(R-33): 결정적 문서 ID, 분실물별 로그 교체, 실패 시 유지, 빈 인덱스 조회, 매핑, 동시성, null 안전 변환.
 * match는 mock 서버(MatchMock)이고 분실물 ID별로 정한 {@code result} JSON을 돌려준다. 외부 호출은 없다.
 */
class MatchingLogReplaceTest extends IntegrationTestBase {

    private static final LocalDate LOST_AT = LocalDate.now().minusDays(3);

    private final ObjectMapper mapper = new ObjectMapper();
    /** 분실물 ID → /matching/findear 의 result JSON (없으면 null) */
    private final Map<String, String> findearResults = new ConcurrentHashMap<>();
    /** 분실물 ID → /matching/lost 의 result JSON (없으면 null) */
    private final Map<String, String> policeResults = new ConcurrentHashMap<>();

    @Autowired
    FindearDataService findearDataService;
    @Autowired
    ElasticsearchOperations operations;
    @Autowired
    MatchingLogIndexMappingChecker mappingChecker;
    @Autowired
    MockMvc mockMvc;
    @Autowired
    MatchingLogWriter matchingLogWriter;

    @BeforeEach
    void insertData() {
        findearResults.clear();
        policeResults.clear();
        insertMember(1);
        insertBoard(10, 1, true, "지갑", "검정", "검은색 가죽 지갑", "검정 가죽 지갑", LocalDateTime.now().minusDays(3));
        insertLostBoard(1, 10, LOST_AT, 126.9707f, 37.5547f);
        insertBoard(20, 1, true, "지갑", "갈색", "갈색 지갑", "갈색 지갑", LocalDateTime.now().minusDays(3));
        insertLostBoard(2, 20, LOST_AT, 126.9707f, 37.5547f);
        mockMatch();
    }

    private void mockMatch() {
        MATCH.respondWith(request -> {
            try {
                JsonNode body = mapper.readTree(request.getBody().utf8());
                String lostBoardId = body.get("lostBoard").get("lostBoardId").asText();
                String path = request.getUrl().encodedPath();
                String result = path.equals("/matching/findear")
                        ? findearResults.getOrDefault(lostBoardId, "null")
                        : policeResults.getOrDefault(lostBoardId, "null");
                return MatchMock.json(200, "{\"message\":\"ok\",\"result\":" + result + "}");
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private LostBoardMatchingDto lostDto(long lostBoardId) {
        return LostBoardMatchingDto.builder()
                .lostBoardId(String.valueOf(lostBoardId)).productName("지갑").color("검정").categoryName("지갑")
                .description("d").lostAt(LOST_AT.toString()).xPos("126.9707").yPos("37.5547").build();
    }

    private static String fItem(long lost, long acquired, double rate) {
        return "{\"lostBoardId\":" + lost + ",\"acquiredBoardId\":" + acquired + ",\"similarityRate\":" + rate + "}";
    }

    private static String pItem(long lost, String atcId, double rate) {
        return "{\"lostBoardId\":" + lost + ",\"acquiredBoardId\":\"" + atcId + "\",\"similarityRate\":" + rate
                + ",\"atcId\":\"" + atcId + "\",\"depPlace\":\"종로경찰서\",\"fdFilePathImg\":\"https://img/" + atcId
                + "\",\"fdPrdtNm\":\"물품\",\"fdSbjt\":\"제목\",\"clrNm\":\"검정\",\"fdYmd\":\"2026-09-30\",\"mainPrdtClNm\":\"지갑\"}";
    }

    private static String list(String... items) {
        return "[" + String.join(",", items) + "]";
    }

    private List<FindearMatchingLog> findearLogs() {
        return StreamSupport.stream(findearMatchingLogRepository.findAll().spliterator(), false).toList();
    }

    private List<PoliceMatchingLog> policeLogs() {
        return StreamSupport.stream(policeMatchingLogRepository.findAll().spliterator(), false).toList();
    }

    private List<String> findearIds() {
        return findearLogs().stream().map(FindearMatchingLog::getFindearMatchingLogId).sorted().toList();
    }

    private List<String> policeIds() {
        return policeLogs().stream().map(PoliceMatchingLog::getPoliceMatchingLogId).sorted().toList();
    }

    @DisplayName("결정적 ID: 같은 분실물을 다시 매칭해도 로그 수는 그대로이고 두 번째 점수·시각으로 갱신된다")
    @Test
    void deterministicIdsOverwrite() throws Exception {
        findearResults.put("1", list(fItem(1, 11, 0.8), fItem(1, 12, 0.6)));
        policeResults.put("1", list(pItem(1, "F-A", 0.7)));

        findearDataService.matchingFindearDatas(lostDto(1));
        FindearMatchingLog before = findearMatchingLogRepository.findById("1-11").orElseThrow();
        assertThat(findearIds()).containsExactly("1-11", "1-12");
        assertThat(policeIds()).containsExactly("1-F-A");

        findearResults.put("1", list(fItem(1, 11, 0.95), fItem(1, 12, 0.6)));
        policeResults.put("1", list(pItem(1, "F-A", 0.5)));
        Thread.sleep(1100); // 시각은 초 단위라 갱신 확인을 위해 1초 넘게 띄운다
        findearDataService.matchingFindearDatas(lostDto(1));

        assertThat(findearIds()).containsExactly("1-11", "1-12");
        assertThat(policeIds()).containsExactly("1-F-A");
        FindearMatchingLog after = findearMatchingLogRepository.findById("1-11").orElseThrow();
        assertThat(after.getSimilarityRate()).isEqualTo(0.95f);
        assertThat(after.getMatchingAt()).isAfter(before.getMatchingAt());
        assertThat(after.getMatchingAt().getNano()).isZero();
        assertThat(policeMatchingLogRepository.findById("1-F-A").orElseThrow().getSimilarityRate()).isEqualTo(0.5f);
        assertThat(findearMatchingLogRepository.count()).isEqualTo(2);
        assertThat(policeMatchingLogRepository.count()).isEqualTo(1);
    }

    @DisplayName("결과와 같아짐: 빠진 후보의 로그는 지워지고, result가 null이면 그 분실물 로그가 모두 지워지며, 다른 분실물의 로그는 그대로")
    @Test
    void logsBecomeEqualToResult() {
        findearResults.put("1", list(fItem(1, 11, 0.8), fItem(1, 12, 0.7), fItem(1, 13, 0.6)));
        findearResults.put("2", list(fItem(2, 11, 0.5)));
        policeResults.put("1", list(pItem(1, "F-A", 0.8), pItem(1, "F-B", 0.7), pItem(1, "F-C", 0.6)));
        policeResults.put("2", list(pItem(2, "F-A", 0.4)));
        findearDataService.matchingFindearDatas(lostDto(1));
        findearDataService.matchingFindearDatas(lostDto(2));
        assertThat(findearIds()).containsExactly("1-11", "1-12", "1-13", "2-11");
        assertThat(policeIds()).containsExactly("1-F-A", "1-F-B", "1-F-C", "2-F-A");

        // 두 번째 매칭에서 match가 2개만 돌려준다
        findearResults.put("1", list(fItem(1, 11, 0.9), fItem(1, 13, 0.6)));
        policeResults.put("1", list(pItem(1, "F-B", 0.9), pItem(1, "F-C", 0.6)));
        findearDataService.matchingFindearDatas(lostDto(1));
        assertThat(findearIds()).containsExactly("1-11", "1-13", "2-11");
        assertThat(policeIds()).containsExactly("1-F-B", "1-F-C", "2-F-A");

        // result: null (후보 없음)이면 분실물 1의 로그가 모두 사라진다. 분실물 2는 그대로
        findearResults.remove("1");
        policeResults.remove("1");
        MatchingAllDatasToAiResDto result = findearDataService.matchingFindearDatas(lostDto(1));
        assertThat(result.getFindearDatas()).isEmpty();
        assertThat(result.getPoliceDatas()).isEmpty();
        assertThat(findearIds()).containsExactly("2-11");
        assertThat(policeIds()).containsExactly("2-F-A");
        assertThat(findearMatchingLogRepository.findById("2-11").orElseThrow().getSimilarityRate()).isEqualTo(0.5f);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parse(String json) throws IOException {
        return mapper.readValue(json, List.class);
    }

    @DisplayName("빈 목록([])은 후보 없음이라 그 분실물의 로그를 모두 지운다 (null과 같다)")
    @Test
    void emptyListDeletesLogs() {
        findearResults.put("1", list(fItem(1, 11, 0.8)));
        policeResults.put("1", list(pItem(1, "F-A", 0.7)));
        findearResults.put("2", list(fItem(2, 11, 0.5)));
        findearDataService.matchingFindearDatas(lostDto(1));
        findearDataService.matchingFindearDatas(lostDto(2));

        assertThat(matchingLogWriter.replaceFindearLogs(1, List.of())).isZero();
        assertThat(matchingLogWriter.replacePoliceLogs(1, List.of())).isZero();

        assertThat(findearIds()).containsExactly("2-11");
        assertThat(policeIds()).isEmpty();
    }

    @DisplayName("항목이 왔는데 유효 항목이 0건이면 upsert도 삭제도 하지 않고 기존 로그를 그대로 둔다 (findear, 0 반환)")
    @Test
    void findearAllInvalidKeepsLogs() throws Exception {
        findearResults.put("1", list(fItem(1, 11, 0.8), fItem(1, 12, 0.6)));
        findearDataService.matchingFindearDatas(lostDto(1));
        FindearMatchingLog before = findearMatchingLogRepository.findById("1-11").orElseThrow();

        int saved = matchingLogWriter.replaceFindearLogs(1, parse(list(
                "{\"lostBoardId\":1,\"similarityRate\":0.5}",
                "{\"lostBoardId\":1,\"acquiredBoardId\":\"x\",\"similarityRate\":0.5}",
                "{\"lostBoardId\":1,\"acquiredBoardId\":13,\"similarityRate\":null}",
                "{\"lostBoardId\":2,\"acquiredBoardId\":14,\"similarityRate\":0.4}")));

        assertThat(saved).isZero();
        assertThat(findearIds()).containsExactly("1-11", "1-12");
        FindearMatchingLog after = findearMatchingLogRepository.findById("1-11").orElseThrow();
        assertThat(after.getSimilarityRate()).isEqualTo(before.getSimilarityRate());
        assertThat(after.getMatchingAt()).isEqualTo(before.getMatchingAt());
    }

    @DisplayName("항목이 왔는데 유효 항목이 0건이면 기존 로그를 그대로 둔다 (police, 0 반환). 일부만 무효면 유효한 것만으로 교체한다")
    @Test
    void policeAllInvalidKeepsLogs() throws Exception {
        policeResults.put("1", list(pItem(1, "F-A", 0.8), pItem(1, "F-B", 0.7)));
        findearDataService.matchingFindearDatas(lostDto(1));
        assertThat(policeIds()).containsExactly("1-F-A", "1-F-B");

        int saved = matchingLogWriter.replacePoliceLogs(1, parse(list(
                "{\"lostBoardId\":1,\"acquiredBoardId\":\"F-C\",\"similarityRate\":0.7}",
                "{\"lostBoardId\":1,\"acquiredBoardId\":\"F-D\",\"similarityRate\":\"abc\",\"atcId\":\"F-D\"}")));

        assertThat(saved).isZero();
        assertThat(policeIds()).containsExactly("1-F-A", "1-F-B");
        assertThat(policeMatchingLogRepository.findById("1-F-A").orElseThrow().getSimilarityRate()).isEqualTo(0.8f);

        // 일부만 무효: 유효한 F-B만 남고 F-A는 지워진다
        saved = matchingLogWriter.replacePoliceLogs(1, parse(list(
                "{\"lostBoardId\":1,\"acquiredBoardId\":\"F-C\",\"similarityRate\":0.7}",
                pItem(1, "F-B", 0.9))));
        assertThat(saved).isEqualTo(1);
        assertThat(policeIds()).containsExactly("1-F-B");
    }

    @DisplayName("결과가 바뀌면 바로 조회에 보인다 (쓰기 직후 refresh)")
    @Test
    void visibleRightAfterWrite() throws Exception {
        findearResults.put("1", list(fItem(1, 11, 0.8)));
        findearDataService.matchingFindearDatas(lostDto(1));
        mockMvc.perform(get("/findear/board/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalCount").value(1))
                .andExpect(jsonPath("$.result.matchingList[0].findearMatchingLogId").value("1-11"));

        findearResults.put("1", list(fItem(1, 12, 0.9), fItem(1, 13, 0.7)));
        findearDataService.matchingFindearDatas(lostDto(1));
        mockMvc.perform(get("/findear/board/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalCount").value(2))
                .andExpect(jsonPath("$.result.matchingList[0].findearMatchingLogId").value("1-12"))
                .andExpect(jsonPath("$.result.matchingList[1].findearMatchingLogId").value("1-13"))
                .andExpect(jsonPath("$.result.matchingList[0].matchedAt").value(org.hamcrest.Matchers.matchesPattern("\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d")));
    }

    @DisplayName("match 호출이 실패하면 예외이고 그 분실물의 기존 로그는 그대로")
    @Test
    void matchFailureKeepsLogs() {
        findearResults.put("1", list(fItem(1, 11, 0.8), fItem(1, 12, 0.6)));
        policeResults.put("1", list(pItem(1, "F-A", 0.7)));
        findearDataService.matchingFindearDatas(lostDto(1));
        List<String> findearBefore = findearIds();
        List<String> policeBefore = policeIds();

        // findear 호출이 500
        MATCH.respondWith(request -> MatchMock.json(500, "{\"message\":\"서버 오류\"}"));
        assertThatThrownBy(() -> findearDataService.matchingFindearDatas(lostDto(1))).isInstanceOf(MatchServerException.class);
        assertThat(findearIds()).isEqualTo(findearBefore);
        assertThat(policeIds()).isEqualTo(policeBefore);
        assertThat(findearMatchingLogRepository.findById("1-11").orElseThrow().getSimilarityRate()).isEqualTo(0.8f);

        // lost112 호출만 500이면 Lost112 로그는 그대로 (findear 로그는 이미 교체된 상태)
        findearResults.put("1", list(fItem(1, 11, 0.9)));
        MATCH.respondWith(request -> request.getUrl().encodedPath().equals("/matching/findear")
                ? MatchMock.json(200, "{\"message\":\"ok\",\"result\":" + findearResults.get("1") + "}")
                : MatchMock.json(500, "{\"message\":\"서버 오류\"}"));
        assertThatThrownBy(() -> findearDataService.matchingFindearDatas(lostDto(1))).isInstanceOf(MatchServerException.class);
        assertThat(findearIds()).containsExactly("1-11");
        assertThat(policeIds()).isEqualTo(policeBefore);
    }

    @DisplayName("빈 인덱스(매핑과 함께 새로 만든 상태)에서도 4개 목록 API가 200, 빈 matchingList, totalCount 0")
    @Test
    void emptyIndexLookups() throws Exception {
        // 인덱스를 지우고 batch가 만드는 것과 같은 매핑으로 다시 만든다 (문서 0건)
        for (Class<?> type : List.of(FindearMatchingLog.class, PoliceMatchingLog.class)) {
            IndexOperations indexOps = operations.indexOps(type);
            if (indexOps.exists()) {
                indexOps.delete();
            }
            indexOps.createWithMapping();
        }
        assertThat(findearMatchingLogRepository.count()).isZero();
        assertThat(policeMatchingLogRepository.count()).isZero();

        for (String path : List.of("/findear/board/1", "/findear/member/1", "/police/board/1", "/police/member/1")) {
            mockMvc.perform(get(path).param("page", "1").param("size", "6"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.matchingList").isArray())
                    .andExpect(jsonPath("$.result.matchingList").isEmpty())
                    .andExpect(jsonPath("$.result.totalCount").value(0));
        }
    }

    @SuppressWarnings("unchecked")
    @DisplayName("매핑: 로그 인덱스 두 개가 지시된 타입으로 만들어지고, 저장된 matchingAt은 초 단위 문자열이며 date로 인덱싱된다")
    @Test
    void mappings() {
        findearResults.put("1", list(fItem(1, 11, 0.8)));
        policeResults.put("1", list(pItem(1, "F-A", 0.7)));
        findearDataService.matchingFindearDatas(lostDto(1));

        Map<String, Object> f = (Map<String, Object>) operations.indexOps(FindearMatchingLog.class).getMapping().get("properties");
        assertThat((Map<String, Object>) f.get("lostBoardId")).containsEntry("type", "long");
        assertThat((Map<String, Object>) f.get("acquiredBoardId")).containsEntry("type", "long");
        assertThat((Map<String, Object>) f.get("similarityRate")).containsEntry("type", "float");
        assertThat((Map<String, Object>) f.get("matchingAt")).containsEntry("type", "date").containsEntry("format", "yyyy-MM-dd'T'HH:mm:ss");

        Map<String, Object> p = (Map<String, Object>) operations.indexOps(PoliceMatchingLog.class).getMapping().get("properties");
        assertThat((Map<String, Object>) p.get("lostBoardId")).containsEntry("type", "long");
        assertThat((Map<String, Object>) p.get("similarityRate")).containsEntry("type", "float");
        assertThat((Map<String, Object>) p.get("matchingAt")).containsEntry("type", "date").containsEntry("format", "yyyy-MM-dd'T'HH:mm:ss");
        for (String keyword : List.of("acquiredBoardId", "atcId", "depPlace", "clrNm", "mainPrdtClNm", "fdPrdtNm")) {
            assertThat((Map<String, Object>) p.get(keyword)).as(keyword).containsEntry("type", "keyword");
        }
        assertThat((Map<String, Object>) p.get("fdFilePathImg")).containsEntry("type", "keyword").containsEntry("index", false);
        assertThat((Map<String, Object>) p.get("fdSbjt")).containsEntry("type", "text");
        assertThat((Map<String, Object>) p.get("fdYmd")).containsEntry("type", "date").containsEntry("format", "yyyy-MM-dd");

        // 저장된 시각 문자열(초 단위)이 date로 인덱싱돼 range 쿼리에 잡힌다
        Map<String, Object> source = operations.get("1-11", Map.class, operations.indexOps(FindearMatchingLog.class).getIndexCoordinates());
        assertThat(source.get("matchingAt").toString()).matches("\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d");
        NativeQuery range = NativeQuery.builder()
                .withQuery(Query.of(q -> q.range(r -> r.date(d -> d.field("matchingAt").gte("2020-01-01T00:00:00")))))
                .build();
        SearchHits<FindearMatchingLog> hits = operations.search(range, FindearMatchingLog.class);
        assertThat(hits.getTotalHits()).isEqualTo(1);
    }

    @DisplayName("동시성: 서로 다른 두 분실물의 매칭을 동시에 돌려도 두 분실물의 로그가 모두 남는다")
    @Test
    void concurrentMatchingKeepsBoth() throws Exception {
        findearResults.put("1", list(fItem(1, 11, 0.8), fItem(1, 12, 0.7), fItem(1, 13, 0.6)));
        findearResults.put("2", list(fItem(2, 11, 0.5), fItem(2, 12, 0.4)));
        policeResults.put("1", list(pItem(1, "F-A", 0.8), pItem(1, "F-B", 0.7)));
        policeResults.put("2", list(pItem(2, "F-A", 0.4)));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 3; round++) {
                CountDownLatch start = new CountDownLatch(1);
                List<Future<?>> futures = new ArrayList<>();
                for (long lostBoardId : new long[]{1, 2}) {
                    futures.add(pool.submit(() -> {
                        start.await();
                        return findearDataService.matchingFindearDatas(lostDto(lostBoardId));
                    }));
                }
                start.countDown();
                for (Future<?> future : futures) {
                    future.get(60, TimeUnit.SECONDS);
                }
                assertThat(findearIds()).containsExactly("1-11", "1-12", "1-13", "2-11", "2-12");
                assertThat(policeIds()).containsExactly("1-F-A", "1-F-B", "2-F-A");
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @DisplayName("null 안전 변환: 값이 null/없는 필드는 null로 저장, atcId가 없는 항목만 건너뛰고 예외 없이 끝나며 응답에도 예외가 없다")
    @Test
    void nullSafeConversion() {
        String full = pItem(1, "F-A", 0.9);
        String nullFields = "{\"lostBoardId\":1,\"acquiredBoardId\":\"F-B\",\"similarityRate\":0.8,\"atcId\":\"F-B\","
                + "\"depPlace\":null,\"fdFilePathImg\":null,\"clrNm\":null,\"fdPrdtNm\":\"물품\",\"mainPrdtClNm\":\"지갑\"}";
        String noAtcId = "{\"lostBoardId\":1,\"acquiredBoardId\":\"F-C\",\"similarityRate\":0.7,\"depPlace\":\"종로경찰서\"}";
        String badRate = "{\"lostBoardId\":1,\"acquiredBoardId\":\"F-D\",\"similarityRate\":\"abc\",\"atcId\":\"F-D\"}";
        policeResults.put("1", list(full, nullFields, noAtcId, badRate));

        MatchingAllDatasToAiResDto result = findearDataService.matchingFindearDatas(lostDto(1));

        // 응답은 match가 준 항목 그대로 (예외 없음)
        assertThat(result.getPoliceDatas()).hasSize(4);
        assertThat(policeIds()).containsExactly("1-F-A", "1-F-B");
        PoliceMatchingLog stored = policeMatchingLogRepository.findById("1-F-B").orElseThrow();
        assertThat(stored.getAtcId()).isEqualTo("F-B");
        assertThat(stored.getDepPlace()).isNull();
        assertThat(stored.getFdFilePathImg()).isNull();
        assertThat(stored.getClrNm()).isNull();
        assertThat(stored.getFdSbjt()).isNull();
        assertThat(stored.getFdYmd()).isNull();
        assertThat(stored.getFdPrdtNm()).isEqualTo("물품");
        assertThat(stored.getSimilarityRate()).isEqualTo(0.8f);
        PoliceMatchingLog complete = policeMatchingLogRepository.findById("1-F-A").orElseThrow();
        assertThat(complete.getFdYmd()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(complete.getDepPlace()).isEqualTo("종로경찰서");
    }

    @DisplayName("findear 결과의 acquiredBoardId·similarityRate가 없거나 숫자가 아닌 항목은 그 항목만 건너뛴다")
    @Test
    void findearBadItemsSkipped() {
        findearResults.put("1", list(fItem(1, 11, 0.8),
                "{\"lostBoardId\":1,\"similarityRate\":0.5}",
                "{\"lostBoardId\":1,\"acquiredBoardId\":\"x\",\"similarityRate\":0.5}",
                "{\"lostBoardId\":1,\"acquiredBoardId\":12,\"similarityRate\":null}",
                fItem(1, 13, 0.6)));

        findearDataService.matchingFindearDatas(lostDto(1));

        assertThat(findearIds()).containsExactly("1-11", "1-13");
    }

    @DisplayName("기동 때 매칭 로그 인덱스 매핑이 옛 매핑이면 인덱스마다 WARN 한 줄, 자동 삭제는 하지 않는다. 정상 매핑이거나 인덱스가 없으면 조용하다")
    @Test
    void warnsWhenMappingDiffers() {
        IndexOperations findearOps = operations.indexOps(FindearMatchingLog.class);
        IndexOperations policeOps = operations.indexOps(PoliceMatchingLog.class);
        Logger logger = (Logger) LoggerFactory.getLogger(MatchingLogIndexMappingChecker.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThat(mappingChecker.check()).isEmpty();
            assertThat(appender.list).filteredOn(e -> e.getLevel() == Level.WARN).isEmpty();

            // R-33 이전 상태 흉내: 매핑이 _class뿐인 인덱스(findear), similarityRate가 double인 인덱스(police)
            findearOps.delete();
            findearOps.create();
            policeOps.delete();
            policeOps.create(Map.of(), Document.from(Map.of("properties", Map.of(
                    "similarityRate", Map.of("type", "double"),
                    "matchingAt", Map.of("type", "date")))));

            List<String> problems = mappingChecker.check();

            assertThat(problems).hasSize(3);
            assertThat(problems).anyMatch(p -> p.startsWith("findear_matching_log.similarityRate: 매핑 없음"));
            assertThat(problems).anyMatch(p -> p.startsWith("findear_matching_log.matchingAt: 매핑 없음"));
            assertThat(problems).anyMatch(p -> p.startsWith("police_matching_log.similarityRate: double"));
            List<ILoggingEvent> warnings = appender.list.stream().filter(e -> e.getLevel() == Level.WARN).toList();
            assertThat(warnings).hasSize(2);
            assertThat(warnings).allSatisfy(e -> assertThat(e.getFormattedMessage())
                    .contains("인덱스 매핑이 다름").contains("인덱스를 지우고 batch를 재기동하세요").contains("docker compose down -v"));
            assertThat(findearOps.exists()).as("자동 삭제하지 않는다").isTrue();
            assertThat(policeOps.exists()).as("자동 삭제하지 않는다").isTrue();

            // 인덱스가 없으면 아무것도 하지 않는다
            appender.list.clear();
            findearOps.delete();
            policeOps.delete();
            assertThat(mappingChecker.check()).isEmpty();
            assertThat(appender.list).filteredOn(e -> e.getLevel() == Level.WARN).isEmpty();
        } finally {
            // 다른 테스트가 쓰는 공유 인덱스를 어노테이션 매핑으로 복구한다
            for (IndexOperations ops : List.of(findearOps, policeOps)) {
                if (ops.exists()) {
                    ops.delete();
                }
                ops.createWithMapping();
            }
            logger.detachAppender(appender);
            appender.stop();
        }
        assertThat(mappingChecker.check()).isEmpty();
    }
}
