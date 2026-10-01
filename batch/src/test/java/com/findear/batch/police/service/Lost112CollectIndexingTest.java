package com.findear.batch.police.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.findear.batch.police.client.Lost112Properties;
import com.findear.batch.police.domain.PoliceAcquiredData;
import com.findear.batch.support.IntegrationTestBase;
import com.findear.batch.support.PoliceDocs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.findear.batch.support.Lost112Fixtures.xml;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lost112 수집 → bulk 인덱싱 통합 테스트 (Testcontainers ES). Lost112는 mock 서버이고 apis.data.go.kr로는 요청이 나가지 않는다.
 * 문서 ID = atcId(upsert), 페이지 단위 처리, 인덱스 매핑, 매핑 불일치 WARN을 확인한다.
 */
class Lost112CollectIndexingTest extends IntegrationTestBase {

    private static final String POLICE_PATH = "/LosfundInfoInqireService/getLosfundInfoAccToClAreaPd";
    private static final IndexCoordinates INDEX = IndexCoordinates.of(PoliceAcquiredData.INDEX);

    @Autowired
    Lost112CollectService service;
    @Autowired
    Lost112Properties properties;
    @Autowired
    ElasticsearchOperations operations;
    @Autowired
    PoliceIndexMappingChecker mappingChecker;

    private String originalKey;
    private int originalPageSize;

    @BeforeEach
    void enableCollect() {
        originalKey = properties.getServiceKey();
        originalPageSize = properties.getPageSize();
        properties.setServiceKey("ab+c/d==");
        properties.setPageSize(2);
    }

    @AfterEach
    void restoreProperties() {
        properties.setServiceKey(originalKey);
        properties.setPageSize(originalPageSize);
    }

    /** 서비스·pageNo에 맞는 픽스처로 응답 (없는 pageNo는 빈 페이지) */
    private void respondWithPages() {
        LOST112.respondWith(request -> {
            String path = request.getUrl().encodedPath();
            String pageNo = request.getUrl().queryParameter("pageNo");
            String prefix = path.equals(POLICE_PATH) ? "police-page" : "portal-page";
            return "1".equals(pageNo) || "2".equals(pageNo) ? xml(prefix + pageNo + ".xml") : xml("no-data.xml");
        });
    }

    @DisplayName("수집: 문서 수 = 유효 item 수(6), 문서 _id = atcId, source에도 id = atcId 문자열, 정규화된 필드")
    @Test
    void collectsIntoIndexWithAtcIdAsDocumentId() {
        respondWithPages();

        Lost112CollectResult result = service.collect();

        assertThat(result.hasFailure()).isFalse();
        assertThat(result.totalIndexed()).isEqualTo(6);
        assertThat(policeAcquiredDataRepository.count()).isEqualTo(6);

        for (String atcId : List.of("F2099010100000001", "F2099010100000002", "F2099010100000003",
                "F2099020100000001", "F2099020100000002", "F2099020100000003")) {
            assertThat(operations.exists(atcId, INDEX)).as("_id=%s", atcId).isTrue();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> source = operations.get("F2099010100000001", Map.class, INDEX);
        assertThat(source).containsEntry("id", "F2099010100000001")
                .containsEntry("atcId", "F2099010100000001")
                .containsEntry("clrNm", "검정")
                .containsEntry("fdYmd", "2026-09-30")
                .containsEntry("prdtClNm", "지갑 > 남성용 지갑")
                .containsEntry("mainPrdtClNm", "지갑")
                .containsEntry("subPrdtClNm", "남성용 지갑")
                .containsEntry("fdSn", "1")
                .containsEntry("source", "POLICE");

        PoliceAcquiredData portal = policeAcquiredDataRepository.findById("F2099020100000001").orElseThrow();
        assertThat(portal.getSource()).isEqualTo("PORTAL");
        assertThat(portal.getFdYmd()).isEqualTo(LocalDate.of(2026, 9, 27));   // yyyyMMdd → yyyy-MM-dd
        assertThat(portal.getMainPrdtClNm()).isEqualTo("지갑");          // 지갑>여성지갑
        assertThat(portal.getSubPrdtClNm()).isEqualTo("여성지갑");
        assertThat(portal.getClrNm()).isEqualTo("은색");

        // 응답에 clrNm이 없는 item: fdSbjt에서 추출 (경찰청 2번째) / 못 찾으면 null (포털기관 2번째)
        assertThat(policeAcquiredDataRepository.findById("F2099010100000002").orElseThrow().getClrNm()).isEqualTo("빨강");
        assertThat(policeAcquiredDataRepository.findById("F2099020100000002").orElseThrow().getClrNm()).isNull();
    }

    @DisplayName("같은 수집을 다시 실행해도 문서 수는 그대로 (upsert), 인덱스를 지우지 않아 기존 문서는 유지된다")
    @Test
    void collectingAgainDoesNotDuplicate() {
        respondWithPages();
        policeAcquiredDataRepository.save(PoliceDocs.doc("F2099999900000001", "지갑", LocalDate.now()));

        service.collect();
        assertThat(policeAcquiredDataRepository.count()).isEqualTo(7);
        service.collect();
        assertThat(policeAcquiredDataRepository.count()).isEqualTo(7);

        assertThat(policeAcquiredDataRepository.findById("F2099999900000001")).isPresent();
    }

    @DisplayName("다시 수집하면 같은 atcId 문서를 새 값으로 덮어쓴다")
    @Test
    void overwritesSameAtcId() {
        respondWithPages();
        policeAcquiredDataRepository.save(PoliceDocs.builder("F2099010100000001", "지갑", LocalDate.now()).fdSbjt("예전 제목").build());

        service.collect();

        PoliceAcquiredData doc = policeAcquiredDataRepository.findById("F2099010100000001").orElseThrow();
        assertThat(doc.getFdSbjt()).startsWith("남성용 반지갑(블랙(검정)색)");
        assertThat(policeAcquiredDataRepository.count()).isEqualTo(6);
    }

    @DisplayName("페이지 단위 처리: mock이 2페이지 요청을 받는 순간 ES에 1페이지 문서가 이미 있다 (2페이지 문서는 아직 없다)")
    @Test
    void indexesPageBeforeRequestingNext() {
        AtomicBoolean checked = new AtomicBoolean();
        AtomicBoolean firstPageAlreadyIndexed = new AtomicBoolean();
        AtomicBoolean secondPageNotYetIndexed = new AtomicBoolean();
        LOST112.respondWith(request -> {
            String path = request.getUrl().encodedPath();
            String pageNo = request.getUrl().queryParameter("pageNo");
            if (path.equals(POLICE_PATH) && "2".equals(pageNo)) {
                firstPageAlreadyIndexed.set(operations.exists("F2099010100000001", INDEX) && operations.exists("F2099010100000002", INDEX));
                secondPageNotYetIndexed.set(!operations.exists("F2099010100000003", INDEX));
                checked.set(true);
            }
            String prefix = path.equals(POLICE_PATH) ? "police-page" : "portal-page";
            return "1".equals(pageNo) || "2".equals(pageNo) ? xml(prefix + pageNo + ".xml") : xml("no-data.xml");
        });

        service.collect();

        assertThat(checked).isTrue();
        assertThat(firstPageAlreadyIndexed).isTrue();
        assertThat(secondPageNotYetIndexed).isTrue();
    }

    @DisplayName("한 서비스가 실패해도 이미 넣은 문서는 유지되고 다른 서비스는 수집된다")
    @Test
    void partialFailureKeepsIndexedDocuments() {
        LOST112.respondWith(request -> {
            String pageNo = request.getUrl().queryParameter("pageNo");
            if (request.getUrl().encodedPath().equals(POLICE_PATH)) {
                return "1".equals(pageNo) ? xml("police-page1.xml") : xml("gateway-22.xml");
            }
            return xml("portal-page" + pageNo + ".xml");
        });

        Lost112CollectResult result = service.collect();

        assertThat(result.services().get(0).error()).isEqualTo("게이트웨이 오류 22 LIMITED_NUMBER_OF_SERVICE_REQUESTS_EXCEEDS_ERROR");
        assertThat(policeAcquiredDataRepository.count()).isEqualTo(5);   // 경찰청 1페이지 2건 + 포털기관 3건
    }

    @SuppressWarnings("unchecked")
    @DisplayName("인덱스 매핑: fdYmd date(yyyy-MM-dd), atcId·mainPrdtClNm keyword, fdFilePathImg index false, depPlace text+keyword")
    @Test
    void indexMapping() {
        Map<String, Object> mapping = operations.indexOps(INDEX).getMapping();
        Map<String, Map<String, Object>> properties = (Map<String, Map<String, Object>>) mapping.get("properties");

        assertThat(properties.get("fdYmd")).containsEntry("type", "date").containsEntry("format", "yyyy-MM-dd");
        assertThat(properties.get("atcId")).containsEntry("type", "keyword");
        assertThat(properties.get("id")).containsEntry("type", "keyword");
        assertThat(properties.get("mainPrdtClNm")).containsEntry("type", "keyword");
        assertThat(properties.get("subPrdtClNm")).containsEntry("type", "keyword");
        assertThat(properties.get("prdtClNm")).containsEntry("type", "keyword");
        assertThat(properties.get("clrNm")).containsEntry("type", "keyword");
        assertThat(properties.get("fdSn")).containsEntry("type", "keyword");
        assertThat(properties.get("source")).containsEntry("type", "keyword");
        assertThat(properties.get("fdFilePathImg")).containsEntry("type", "keyword").containsEntry("index", false);
        assertThat(properties.get("depPlace")).containsEntry("type", "text");
        assertThat((Map<String, Object>) properties.get("depPlace").get("fields")).containsKey("keyword");
        assertThat(properties.get("fdPrdtNm")).containsEntry("type", "text");
        assertThat((Map<String, Object>) properties.get("fdPrdtNm").get("fields")).containsKey("keyword");
        assertThat(properties.get("fdSbjt")).containsEntry("type", "text");
        assertThat(properties.get("addr")).containsEntry("type", "text");

        assertThat(mappingChecker.check()).isEmpty();
    }

    @DisplayName("매핑이 다른 기존 인덱스(옛 동적 매핑)면 WARN 한 줄, 자동 삭제는 하지 않는다. 인덱스가 없으면 조용히 통과")
    @Test
    void warnsWhenMappingDiffers() {
        IndexOperations indexOps = operations.indexOps(PoliceAcquiredData.class);
        Logger logger = (Logger) LoggerFactory.getLogger(PoliceIndexMappingChecker.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        Level original = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        try {
            // 옛 매핑 흉내: atcId text, fdYmd keyword
            indexOps.delete();
            indexOps.create(Map.of(), Document.from(Map.of("properties", Map.of(
                    "atcId", Map.of("type", "text"),
                    "mainPrdtClNm", Map.of("type", "keyword"),
                    "fdYmd", Map.of("type", "keyword")))));

            List<String> problems = mappingChecker.check();

            assertThat(problems).hasSize(2);
            assertThat(problems).anyMatch(p -> p.startsWith("atcId: text"));
            assertThat(problems).anyMatch(p -> p.startsWith("fdYmd: keyword"));
            List<ILoggingEvent> warnings = appender.list.stream().filter(e -> e.getLevel() == Level.WARN).toList();
            assertThat(warnings).hasSize(1);
            assertThat(warnings.get(0).getFormattedMessage())
                    .contains("police_acquired_data 인덱스 매핑이 다름")
                    .contains("인덱스를 지우고 batch를 재기동하세요")
                    .contains("docker compose down -v");
            assertThat(indexOps.exists()).as("자동 삭제하지 않는다").isTrue();

            // 인덱스가 없으면 아무것도 하지 않는다
            appender.list.clear();
            indexOps.delete();
            assertThat(mappingChecker.check()).isEmpty();
            assertThat(appender.list).filteredOn(e -> e.getLevel() == Level.WARN).isEmpty();
        } finally {
            // 다른 테스트가 쓰는 공유 인덱스를 어노테이션 매핑으로 복구한다
            if (indexOps.exists()) {
                indexOps.delete();
            }
            indexOps.createWithMapping();
            logger.detachAppender(appender);
            logger.setLevel(original);
            appender.stop();
        }
        assertThat(mappingChecker.check()).isEmpty();
    }
}
