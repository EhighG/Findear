package com.findear.batch.police.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;
import com.findear.batch.police.client.Lost112ApiService;
import com.findear.batch.police.client.Lost112Client;
import com.findear.batch.police.client.Lost112Properties;
import com.findear.batch.police.client.Lost112XmlParser;
import com.findear.batch.police.domain.PoliceAcquiredData;
import com.findear.batch.police.exception.Lost112NotConfiguredException;
import com.findear.batch.police.exception.Lost112UnavailableException;
import com.findear.batch.support.MatchMock;
import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.net.ServerSocket;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.findear.batch.support.Lost112Fixtures.text;
import static com.findear.batch.support.Lost112Fixtures.xml;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Lost112 클라이언트·수집 계약 테스트. Lost112는 호출하지 않고 mock 서버(mockwebserver3)만 쓴다 (D-38).
 * 인덱싱은 Mockito 가짜(받은 문서를 모아 둠)로 대신해 DB·ES 없이 요청 모양과 수집 흐름만 확인한다.
 */
class Lost112CollectContractTest {

    private static final String POLICE_PATH = "/LosfundInfoInqireService/getLosfundInfoAccToClAreaPd";
    private static final String PORTAL_PATH = "/LosPtfundInfoInqireService/getPtLosfundInfoAccToClAreaPd";
    private static final String KEY = "ab+c/d==";
    private static final String ENCODED_KEY = "ab%2Bc%2Fd%3D%3D";
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private static final MatchMock LOST112 = new MatchMock();

    private Lost112Properties properties;
    private PoliceAcquiredDataIndexer indexer;
    private List<PoliceAcquiredData> indexed;
    private List<Integer> indexCalls;
    private Lost112CollectService service;

    private ListAppender<ILoggingEvent> logs;
    private Logger appLogger;

    @BeforeEach
    void setUp() {
        LOST112.reset();
        properties = new Lost112Properties();
        properties.setBaseUrl(LOST112.url());
        properties.setServiceKey(KEY);
        properties.setPageSize(2);
        service = newService(properties);

        appLogger = (Logger) LoggerFactory.getLogger("com.findear.batch");
        appLogger.setLevel(Level.DEBUG);
        logs = new ListAppender<>();
        logs.start();
        appLogger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        appLogger.detachAppender(logs);
        logs.stop();
    }

    private Lost112CollectService newService(Lost112Properties props) {
        indexer = mock(PoliceAcquiredDataIndexer.class);
        indexed = new ArrayList<>();
        indexCalls = new ArrayList<>();
        doAnswer(invocation -> {
            List<PoliceAcquiredData> documents = invocation.getArgument(0);
            indexed.addAll(documents);
            indexCalls.add(documents.size());
            return null;
        }).when(indexer).index(anyList());

        var restTemplate = new RestTemplateBuilder()
                .connectTimeout(props.getConnectTimeout())
                .readTimeout(props.getReadTimeout())
                .build();
        return new Lost112CollectService(new Lost112Client(restTemplate, props), new Lost112XmlParser(),
                new PoliceDataNormalizer(), indexer, props);
    }

    /** 서비스·pageNo에 맞는 픽스처로 응답하는 mock. 픽스처가 없는 pageNo는 빈 페이지(resultCode 03) */
    private void respondWithPages() {
        LOST112.respondWith(request -> {
            String path = request.getUrl().encodedPath();
            String pageNo = request.getUrl().queryParameter("pageNo");
            String prefix = path.equals(POLICE_PATH) ? "police-page" : path.equals(PORTAL_PATH) ? "portal-page" : null;
            if (prefix == null || pageNo == null || !(pageNo.equals("1") || pageNo.equals("2"))) {
                return xml("no-data.xml");
            }
            return xml(prefix + pageNo + ".xml");
        });
    }

    private List<RecordedRequest> requestsTo(String path) {
        return LOST112.requests().stream().filter(r -> r.getUrl().encodedPath().equals(path)).toList();
    }

    private String logText() {
        StringBuilder text = new StringBuilder();
        for (ILoggingEvent event : logs.list) {
            text.append(event.getFormattedMessage()).append('\n');
            if (event.getThrowableProxy() != null) {
                text.append(ThrowableProxyUtil.asString(event.getThrowableProxy())).append('\n');
            }
        }
        return text.toString();
    }

    @DisplayName("경로: 경찰청 → 포털기관 순으로 서비스별 오퍼레이션 경로를 요청한다")
    @Test
    void pathsPerService() {
        respondWithPages();

        service.collect();

        List<String> paths = LOST112.requests().stream().map(r -> r.getUrl().encodedPath()).toList();
        assertThat(paths).containsExactly(POLICE_PATH, POLICE_PATH, PORTAL_PATH, PORTAL_PATH);
        assertThat(LOST112.requests()).allSatisfy(r -> assertThat(r.getMethod()).isEqualTo("GET"));
    }

    @DisplayName("파라미터: serviceKey·pageNo·numOfRows·START_YMD·END_YMD만 (비어 있는 옵션은 보내지 않음), 날짜는 서울 기준 오늘-N일 ~ 오늘")
    @Test
    void parameters() {
        respondWithPages();
        properties.setCollectDays(7);
        LocalDate today = LocalDate.now(SEOUL);

        Lost112CollectResult result = service.collect();

        RecordedRequest first = LOST112.requests().get(0);
        Set<String> names = new LinkedHashSet<>();
        for (String pair : first.getUrl().encodedQuery().split("&")) {
            names.add(pair.substring(0, pair.indexOf('=')));
        }
        assertThat(names).containsExactlyInAnyOrder("serviceKey", "pageNo", "numOfRows", "START_YMD", "END_YMD");
        assertThat(first.getUrl().queryParameter("numOfRows")).isEqualTo("2");
        String start = today.minusDays(7).format(DateTimeFormatter.BASIC_ISO_DATE);
        String end = today.format(DateTimeFormatter.BASIC_ISO_DATE);
        assertThat(first.getUrl().queryParameter("START_YMD")).isEqualTo(start);
        assertThat(first.getUrl().queryParameter("END_YMD")).isEqualTo(end);
        assertThat(result.startYmd()).isEqualTo(start);
        assertThat(result.endYmd()).isEqualTo(end);
        // 색상·분류·지역 파라미터는 쓰지 않는다
        assertThat(first.getUrl().encodedQuery()).doesNotContain("CLR_CD", "FD_COL_CD", "PRDT_CL_CD", "N_FD_LCT_CD");
    }

    @DisplayName("serviceKey는 한 번만 인코딩: Decoding 키 ab+c/d==는 ab%2Bc%2Fd%3D%3D")
    @Test
    void serviceKeyEncodedOnce() {
        respondWithPages();

        service.collect();

        for (RecordedRequest request : LOST112.requests()) {
            assertThat(request.getUrl().encodedQuery()).contains("serviceKey=" + ENCODED_KEY + "&").doesNotContain("%252");
            assertThat(request.getUrl().queryParameter("serviceKey")).isEqualTo(KEY);
        }
    }

    @DisplayName("Encoding 키(%XX 포함)를 넣어도 같은 쿼리가 나간다")
    @Test
    void encodingKeyIsAccepted() {
        respondWithPages();
        properties.setServiceKey(ENCODED_KEY);

        service.collect();

        for (RecordedRequest request : LOST112.requests()) {
            assertThat(request.getUrl().encodedQuery()).contains("serviceKey=" + ENCODED_KEY + "&").doesNotContain("%252");
            assertThat(request.getUrl().queryParameter("serviceKey")).isEqualTo(KEY);
        }
    }

    @DisplayName("특수문자가 없는 평범한 키는 그대로, 앞뒤 공백은 제거")
    @Test
    void plainKey() {
        respondWithPages();
        properties.setServiceKey("  plainKey123  ");

        service.collect();

        assertThat(LOST112.requests().get(0).getUrl().queryParameter("serviceKey")).isEqualTo("plainKey123");
    }

    @DisplayName("totalCount 3, numOfRows 2: 서비스마다 정확히 2페이지(pageNo 1, 2)를 요청하고 멈춘다. 유효 item만 인덱싱")
    @Test
    void stopsAfterTotalCount() {
        respondWithPages();

        Lost112CollectResult result = service.collect();

        assertThat(requestsTo(POLICE_PATH)).extracting(r -> r.getUrl().queryParameter("pageNo")).containsExactly("1", "2");
        assertThat(requestsTo(PORTAL_PATH)).extracting(r -> r.getUrl().queryParameter("pageNo")).containsExactly("1", "2");

        assertThat(result.hasFailure()).isFalse();
        assertThat(result.services()).hasSize(2);
        Lost112CollectResult.ServiceResult police = result.services().get(0);
        assertThat(police.service()).isEqualTo(Lost112ApiService.POLICE);
        assertThat(police.pages()).isEqualTo(2);
        assertThat(police.fetched()).isEqualTo(4);   // 2 + 2 (atcId 없는 item 포함)
        assertThat(police.indexed()).isEqualTo(3);
        assertThat(police.skipped()).isEqualTo(1);   // atcId가 없는 item
        Lost112CollectResult.ServiceResult portal = result.services().get(1);
        assertThat(portal.pages()).isEqualTo(2);
        assertThat(portal.fetched()).isEqualTo(3);
        assertThat(portal.indexed()).isEqualTo(3);
        assertThat(portal.skipped()).isZero();
        assertThat(result.totalIndexed()).isEqualTo(6);

        // 페이지마다 따로 bulk 인덱싱 (경찰청 2·1건, 포털기관 2·1건)
        assertThat(indexCalls).containsExactly(2, 1, 2, 1);
        assertThat(indexed).extracting(PoliceAcquiredData::getId).containsExactly(
                "F2099010100000001", "F2099010100000002", "F2099010100000003",
                "F2099020100000001", "F2099020100000002", "F2099020100000003");
        assertThat(indexed).extracting(PoliceAcquiredData::getSource).containsExactly(
                "POLICE", "POLICE", "POLICE", "PORTAL", "PORTAL", "PORTAL");
    }

    @DisplayName("요약 로그: 서비스별 INFO 한 줄")
    @Test
    void summaryLogLines() {
        respondWithPages();

        service.collect();

        assertThat(logs.list).filteredOn(e -> e.getLevel() == Level.INFO)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anyMatch(m -> m.startsWith("Lost112 수집 POLICE: pages=2, fetched=4, indexed=3, skipped=1"))
                .anyMatch(m -> m.startsWith("Lost112 수집 PORTAL: pages=2, fetched=3, indexed=3, skipped=0"));
    }

    @DisplayName("빈 items(또는 resultCode 03)면 첫 페이지에서 멈춘다")
    @Test
    void stopsOnEmptyItems() {
        LOST112.respondWith(request -> request.getUrl().encodedPath().equals(POLICE_PATH) ? xml("empty-items.xml") : xml("no-data.xml"));

        Lost112CollectResult result = service.collect();

        assertThat(LOST112.requests()).hasSize(2);
        assertThat(result.hasFailure()).isFalse();
        assertThat(result.totalIndexed()).isZero();
        assertThat(indexed).isEmpty();
    }

    @DisplayName("응답에 totalCount가 없으면 빈 페이지가 나올 때까지 계속한다")
    @Test
    void withoutTotalCountContinuesUntilEmpty() {
        String noTotal = text("police-page1.xml").replace("<totalCount>3</totalCount>", "");
        LOST112.respondWith(request -> {
            boolean first = "1".equals(request.getUrl().queryParameter("pageNo"));
            return first && request.getUrl().encodedPath().equals(POLICE_PATH) ? xml(200, noTotal) : xml("no-data.xml");
        });

        Lost112CollectResult result = service.collect();

        assertThat(requestsTo(POLICE_PATH)).hasSize(2);
        assertThat(requestsTo(PORTAL_PATH)).hasSize(1);
        assertThat(result.services().get(0).indexed()).isEqualTo(2);
    }

    @DisplayName("max-pages 상한: 닿으면 멈추고 WARN, 결과에 truncated")
    @Test
    void maxPagesLimit() {
        String endless = "<response><header><resultCode>00</resultCode></header><body><items>"
                + "<item><atcId>F2099999900000001</atcId><fdYmd>2026-09-30</fdYmd></item>"
                + "</items><numOfRows>2</numOfRows><totalCount>100000</totalCount></body></response>";
        LOST112.respondWith(request -> xml(200, endless));
        properties.setMaxPages(3);

        Lost112CollectResult result = service.collect();

        assertThat(requestsTo(POLICE_PATH)).hasSize(3);
        assertThat(requestsTo(PORTAL_PATH)).hasSize(3);
        assertThat(result.services()).allSatisfy(r -> {
            assertThat(r.pages()).isEqualTo(3);
            assertThat(r.truncated()).isTrue();
            assertThat(r.failed()).isFalse();
        });
        assertThat(logs.list).filteredOn(e -> e.getLevel() == Level.WARN)
                .extracting(ILoggingEvent::getFormattedMessage).anyMatch(m -> m.contains("max-pages(3)"));
    }

    @DisplayName("키가 비어 있으면 요청을 한 건도 보내지 않고 미설정 예외")
    @Test
    void noKeyNoRequests() {
        properties.setServiceKey("");

        assertThatThrownBy(() -> service.collect()).isInstanceOf(Lost112NotConfiguredException.class)
                .hasMessage("Lost112 API 키가 설정되지 않았습니다.");
        properties.setServiceKey("   ");
        assertThatThrownBy(() -> service.collectOrThrow()).isInstanceOf(Lost112NotConfiguredException.class);

        assertThat(LOST112.requests()).isEmpty();
        assertThat(indexed).isEmpty();
    }

    @DisplayName("경찰청이 HTTP 500을 줘도 그 서비스만 멈추고 포털기관은 수집한다 (요약에 오류)")
    @Test
    void oneServiceHttpError() {
        LOST112.respondWith(request -> {
            if (request.getUrl().encodedPath().equals(POLICE_PATH)) {
                return xml(500, "<html>error</html>");
            }
            return xml("portal-page" + request.getUrl().queryParameter("pageNo") + ".xml");
        });

        Lost112CollectResult result = service.collect();

        assertThat(requestsTo(POLICE_PATH)).hasSize(1);
        assertThat(requestsTo(PORTAL_PATH)).hasSize(2);
        assertThat(result.services().get(0).error()).isEqualTo("HTTP 500");
        assertThat(result.services().get(0).indexed()).isZero();
        assertThat(result.services().get(1).failed()).isFalse();
        assertThat(result.services().get(1).indexed()).isEqualTo(3);
        assertThat(result.hasFailure()).isTrue();
        assertThat(result.totalFailure()).isFalse();
        assertThat(result.failureSummary()).isEqualTo("POLICE: HTTP 500");
        // 일부라도 넣었으면 collectOrThrow는 예외 없이 요약을 돌려준다
        assertThat(service.collectOrThrow().totalIndexed()).isEqualTo(3);
    }

    @DisplayName("게이트웨이 오류 30(키 미등록)은 그 서비스만 실패 — 이미 넣은 페이지는 유지하고 다음 서비스는 계속")
    @Test
    void gatewayErrorMidway() {
        LOST112.respondWith(request -> {
            String pageNo = request.getUrl().queryParameter("pageNo");
            if (request.getUrl().encodedPath().equals(POLICE_PATH)) {
                return pageNo.equals("1") ? xml("police-page1.xml") : xml("gateway-30.xml");
            }
            return xml("portal-page" + pageNo + ".xml");
        });

        Lost112CollectResult result = service.collect();

        Lost112CollectResult.ServiceResult police = result.services().get(0);
        assertThat(police.error()).isEqualTo("게이트웨이 오류 30 SERVICE_KEY_IS_NOT_REGISTERED_ERROR");
        assertThat(police.pages()).isEqualTo(1);
        assertThat(police.indexed()).isEqualTo(2);   // 1페이지는 이미 인덱싱됨
        assertThat(result.services().get(1).indexed()).isEqualTo(3);
        assertThat(indexed).hasSize(5);
        assertThat(result.totalFailure()).isFalse();
    }

    @DisplayName("모든 서비스가 실패해 하나도 못 넣으면 collectOrThrow는 Lost112UnavailableException (원인 코드, 키 없음)")
    @Test
    void totalFailure() {
        LOST112.respondWith(request -> request.getUrl().encodedPath().equals(POLICE_PATH) ? xml("gateway-30.xml") : xml(503, "unavailable"));

        Lost112CollectResult result = service.collect();
        assertThat(result.totalFailure()).isTrue();

        assertThatThrownBy(() -> service.collectOrThrow())
                .isInstanceOf(Lost112UnavailableException.class)
                .hasMessage("Lost112 호출에 실패했습니다: POLICE: 게이트웨이 오류 30 SERVICE_KEY_IS_NOT_REGISTERED_ERROR; PORTAL: HTTP 503")
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(KEY, ENCODED_KEY));
    }

    @DisplayName("결과가 구조에 맞지 않는 XML이면 실패로 집계")
    @Test
    void malformedResponse() {
        LOST112.respondWith(request -> xml(200, text("not-xml.txt")));

        Lost112CollectResult result = service.collect();

        assertThat(result.services()).allSatisfy(r -> assertThat(r.error()).contains("XML로 읽을 수 없는"));
        assertThat(result.totalFailure()).isTrue();
    }

    @DisplayName("시간 초과: 실패로 집계하고 예외 메시지·로그에 키가 없다")
    @Test
    void timeoutDoesNotLeakKey() {
        LOST112.respondWith(request -> new MockResponse.Builder().headersDelay(3, TimeUnit.SECONDS).build());
        properties.setReadTimeout(Duration.ofMillis(300));
        service = newService(properties);

        Lost112CollectResult result = service.collect();

        assertThat(result.services()).allSatisfy(r -> assertThat(r.error()).startsWith("연결 실패 또는 시간 초과 ("));
        assertThat(result.totalFailure()).isTrue();
        assertThatThrownBy(() -> service.collectOrThrow()).satisfies(e -> assertThat(e.getMessage()).doesNotContain(KEY, ENCODED_KEY, "serviceKey"));
        assertThat(logText()).doesNotContain(KEY, ENCODED_KEY, "serviceKey");
    }

    @DisplayName("연결 불가: 실패로 집계하고 예외 메시지·로그에 키가 없다")
    @Test
    void connectionRefusedDoesNotLeakKey() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        properties.setBaseUrl("http://localhost:" + closedPort);

        Lost112CollectResult result = service.collect();

        assertThat(result.services()).allSatisfy(r -> assertThat(r.error()).startsWith("연결 실패 또는 시간 초과 ("));
        assertThat(result.totalFailure()).isTrue();
        assertThatThrownBy(() -> service.collectOrThrow()).satisfies(e -> assertThat(e.getMessage()).doesNotContain(KEY, ENCODED_KEY, "serviceKey"));
        assertThat(logText()).doesNotContain(KEY, ENCODED_KEY, "serviceKey");
    }

    @DisplayName("정상 수집 로그에도 키가 없다")
    @Test
    void successLogsDoNotContainKey() {
        respondWithPages();

        service.collect();

        assertThat(logText()).isNotBlank().doesNotContain(KEY, ENCODED_KEY, "serviceKey");
    }

    @DisplayName("Lost112Client.decodingKey: %XX가 있으면 한 번 풀고('+'는 공백이 되지 않음), 없으면 그대로")
    @Test
    void decodingKey() {
        assertThat(Lost112Client.decodingKey("ab%2Bc%2Fd%3D%3D")).isEqualTo("ab+c/d==");
        assertThat(Lost112Client.decodingKey("ab+c/d==")).isEqualTo("ab+c/d==");
        assertThat(Lost112Client.decodingKey("a+b%2Fc")).isEqualTo("a+b/c");
        assertThat(Lost112Client.decodingKey(null)).isEqualTo("");
    }
}
