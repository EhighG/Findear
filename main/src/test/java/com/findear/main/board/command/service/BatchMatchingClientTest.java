package com.findear.main.board.command.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findear.main.board.query.dto.BatchServerResponseDto;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * batch `POST /findear/matching` 계약 테스트 (07 §2). batch는 호출하지 않고 mock 서버로 요청 모양과 응답·실패 처리를 확인한다.
 * 알림 서비스는 mock이라 DB는 쓰지 않는다 (응답 모양별 알림 여부는 LostBoardMatchingAlertServiceTest).
 */
class BatchMatchingClientTest {

    private static final LostBoardMatchingRequestedEvent EVENT = new LostBoardMatchingRequestedEvent(
            7L, "검정 가죽 지갑", "검정", "지갑", "가죽 소형 로고", LocalDate.of(2026, 9, 29), 126.97f, 37.55f);

    private static final String OK_BODY = """
            {"status":200,"message":"습득물 매칭 성공","result":{
              "findearDatas":[{"lostBoardId":7,"acquiredBoardId":3,"similarityRate":0.93}],
              "policeDatas":[{"lostBoardId":7,"acquiredBoardId":"F2099100100000000001","similarityRate":0.81}]}}""";

    /** 첫 요청은 Netty 초기화로 느릴 수 있어 일반 테스트는 넉넉한 상한을, 시간 초과 테스트만 짧은 상한을 쓴다. */
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Duration SHORT_TIMEOUT = Duration.ofMillis(300);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockWebServer batch;
    private LostBoardMatchingAlertService alertService;
    private BatchMatchingClient client;

    @BeforeEach
    void setUp() throws IOException {
        batch = new MockWebServer();
        batch.start();
        alertService = mock(LostBoardMatchingAlertService.class);
        client = clientFor(batch.url("/").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        batch.close();
    }

    private BatchMatchingClient clientFor(String baseUrl) {
        return clientFor(baseUrl, TIMEOUT);
    }

    private BatchMatchingClient clientFor(String baseUrl, Duration timeout) {
        return new BatchMatchingClient(WebClient.builder(), alertService, baseUrl, timeout);
    }

    private static MockResponse json(int code, String body) {
        return new MockResponse.Builder().code(code).setHeader("Content-Type", "application/json").body(body).build();
    }

    @DisplayName("요청: POST /findear/matching, application/json, 본문 키는 lostBoardId·productName·color·categoryName·description·lostAt·xpos·ypos")
    @Test
    void requestShape() throws Exception {
        batch.enqueue(json(200, OK_BODY));

        client.matching(EVENT).block(Duration.ofSeconds(5));

        RecordedRequest request = batch.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getUrl().encodedPath()).isEqualTo("/findear/matching");
        MediaType contentType = MediaType.parseMediaType(request.getHeaders().get("Content-Type"));
        assertThat(contentType.isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
        assertThat(contentType.getCharset()).isIn(null, java.nio.charset.StandardCharsets.UTF_8);

        String body = request.getBody().utf8();
        JsonNode json = objectMapper.readTree(body);
        List<String> keys = new java.util.ArrayList<>();
        json.fieldNames().forEachRemaining(keys::add);
        assertThat(keys).containsExactlyInAnyOrder(
                "lostBoardId", "productName", "color", "categoryName", "description", "lostAt", "xpos", "ypos");
        assertThat(json.get("lostBoardId").asLong()).isEqualTo(7L);
        assertThat(json.get("productName").asText()).isEqualTo("검정 가죽 지갑");
        assertThat(json.get("color").asText()).isEqualTo("검정");
        assertThat(json.get("categoryName").asText()).isEqualTo("지갑");
        assertThat(json.get("description").asText()).isEqualTo("가죽 소형 로고");
        assertThat(json.get("lostAt").asText()).isEqualTo("2026-09-29");
        assertThat(json.get("xpos").floatValue()).isEqualTo(126.97f);
        assertThat(json.get("ypos").floatValue()).isEqualTo(37.55f);
        assertThat(body).contains("검정 가죽 지갑");
    }

    @DisplayName("07 §2 모양의 응답은 이벤트의 분실물 id와 함께 알림 서비스에 1번 넘긴다")
    @Test
    void successHandsResponseToAlertService() {
        batch.enqueue(json(200, OK_BODY));

        client.matching(EVENT).block(Duration.ofSeconds(5));

        ArgumentCaptor<BatchServerResponseDto> response = ArgumentCaptor.forClass(BatchServerResponseDto.class);
        verify(alertService).alertIfMatched(eq(7L), response.capture());
        assertThat(response.getValue().getStatus()).isEqualTo(200);
        assertThat(response.getValue().getResult()).isInstanceOf(java.util.Map.class);
    }

    @DisplayName("결과가 빈 목록이거나 result가 null인 200도 알림 서비스가 판단하도록 그대로 넘기고 예외는 없다")
    @Test
    void emptyAndNullResultAreForwarded() {
        batch.enqueue(json(200, "{\"status\":200,\"message\":\"ok\",\"result\":{\"findearDatas\":[],\"policeDatas\":[]}}"));
        batch.enqueue(json(200, "{\"status\":200,\"message\":\"ok\",\"result\":null}"));

        assertThatCode(() -> client.matching(EVENT).block(Duration.ofSeconds(5))).doesNotThrowAnyException();
        assertThatCode(() -> client.matching(EVENT).block(Duration.ofSeconds(5))).doesNotThrowAnyException();

        verify(alertService, org.mockito.Mockito.times(2)).alertIfMatched(eq(7L), any(BatchServerResponseDto.class));
    }

    @DisplayName("requestMatching은 바로 돌아오고 응답이 오면 비동기로 알림 서비스를 부른다")
    @Test
    void requestMatchingIsAsync() {
        batch.enqueue(json(200, OK_BODY));

        client.requestMatching(EVENT);

        verify(alertService, timeout(5_000)).alertIfMatched(eq(7L), any(BatchServerResponseDto.class));
    }

    @DisplayName("404는 알림하지 않고 예외도 밖으로 나오지 않는다")
    @Test
    void notFoundIsSwallowed() {
        batch.enqueue(json(404, "{\"status\":404,\"message\":\"not found\"}"));
        assertFailureSwallowed();
    }

    @DisplayName("500은 알림하지 않고 예외도 밖으로 나오지 않는다")
    @Test
    void serverErrorIsSwallowed() {
        batch.enqueue(json(500, "{\"status\":500,\"message\":\"boom\"}"));
        assertFailureSwallowed();
    }

    @DisplayName("깨진 JSON은 알림하지 않고 예외도 밖으로 나오지 않는다")
    @Test
    void brokenJsonIsSwallowed() {
        batch.enqueue(json(200, "{\"status\":200,\"result\":{\"findearDatas\":["));
        assertFailureSwallowed();
    }

    @DisplayName("본문이 없는 200은 알림하지 않는다")
    @Test
    void emptyBodyIsSkipped() {
        batch.enqueue(new MockResponse.Builder().code(200).build());
        assertFailureSwallowed();
    }

    @DisplayName("설정한 시간보다 늦은 응답은 알림하지 않는다")
    @Test
    void slowResponseTimesOut() {
        batch.enqueue(new MockResponse.Builder().code(200).setHeader("Content-Type", "application/json")
                .body(OK_BODY).headersDelay(2, TimeUnit.SECONDS).build());

        client = clientFor(batch.url("/").toString(), SHORT_TIMEOUT);

        long start = System.currentTimeMillis();
        assertFailureSwallowed();
        assertThat(System.currentTimeMillis() - start).isLessThan(1_900);
    }

    @DisplayName("연결할 수 없으면 알림하지 않는다")
    @Test
    void connectionRefusedIsSwallowed() throws IOException {
        String deadUrl = batch.url("/").toString();
        batch.close();
        client = clientFor(deadUrl);

        assertFailureSwallowed();
    }

    @DisplayName("알림 서비스가 예외를 던져도 밖으로 나오지 않는다")
    @Test
    void alertFailureIsSwallowed() {
        batch.enqueue(json(200, OK_BODY));
        org.mockito.Mockito.doThrow(new IllegalStateException("boom")).when(alertService)
                .alertIfMatched(any(), any());

        assertThatCode(() -> client.matching(EVENT).block(Duration.ofSeconds(5))).doesNotThrowAnyException();
    }

    private void assertFailureSwallowed() {
        assertThatCode(() -> client.matching(EVENT).block(Duration.ofSeconds(5))).doesNotThrowAnyException();
        verify(alertService, never()).alertIfMatched(any(), any());
    }
}
