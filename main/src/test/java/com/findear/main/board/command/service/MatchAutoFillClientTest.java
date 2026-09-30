package com.findear.main.board.command.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.findear.main.board.command.dto.AiGeneratedColumnDto;
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
 * match `POST /process` 계약 테스트 (07 §5.1). match는 호출하지 않고 mock 서버로 요청 모양과 응답·실패 처리를 확인한다.
 * 반영 서비스는 mock이라 DB는 쓰지 않는다.
 */
class MatchAutoFillClientTest {

    private static final AutoFillRequestedEvent EVENT = new AutoFillRequestedEvent(
            7L, "검정 가죽 지갑", "http://localhost:8333/findear-images/images/2026/10/3f2b8c1e-1111-4222-8333-444455556666.png");

    private static final String OK_BODY = """
            {"message":"success","result":{"category":"지갑","color":"검정","description":["검정","가죽","지갑","소형","로고"]}}""";

    private static final Duration TIMEOUT = Duration.ofMillis(300);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockWebServer match;
    private AcquiredBoardAutoFillService autoFillService;
    private MatchAutoFillClient client;

    @BeforeEach
    void setUp() throws IOException {
        match = new MockWebServer();
        match.start();
        autoFillService = mock(AcquiredBoardAutoFillService.class);
        client = clientFor(match.url("/").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        match.close();
    }

    private MatchAutoFillClient clientFor(String baseUrl) {
        return new MatchAutoFillClient(WebClient.builder(), autoFillService, baseUrl, TIMEOUT);
    }

    private static MockResponse json(int code, String body) {
        return new MockResponse.Builder().code(code).setHeader("Content-Type", "application/json").body(body).build();
    }

    @DisplayName("요청: POST /process, application/json, 본문은 productName·imgUrl (한글 UTF-8 그대로)")
    @Test
    void requestShape() throws Exception {
        match.enqueue(json(200, OK_BODY));

        client.autoFill(EVENT).block(Duration.ofSeconds(5));

        RecordedRequest request = match.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getUrl().encodedPath()).isEqualTo("/process");
        MediaType contentType = MediaType.parseMediaType(request.getHeaders().get("Content-Type"));
        assertThat(contentType.isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
        assertThat(contentType.getCharset()).isIn(null, java.nio.charset.StandardCharsets.UTF_8);

        String body = request.getBody().utf8();
        JsonNode expected = objectMapper.readTree(
                "{\"productName\":\"검정 가죽 지갑\",\"imgUrl\":\"" + EVENT.imgUrl() + "\"}");
        assertThat(objectMapper.readTree(body)).isEqualTo(expected);
        assertThat(body).contains("검정 가죽 지갑");
    }

    @DisplayName("07 §5.1 모양의 응답은 반영 서비스에 그 값으로 1번 넘긴다")
    @Test
    void successAppliesResult() {
        match.enqueue(json(200, OK_BODY));

        client.autoFill(EVENT).block(Duration.ofSeconds(5));

        ArgumentCaptor<AiGeneratedColumnDto> result = ArgumentCaptor.forClass(AiGeneratedColumnDto.class);
        verify(autoFillService).apply(eq(7L), result.capture());
        assertThat(result.getValue().getCategory()).isEqualTo("지갑");
        assertThat(result.getValue().getColor()).isEqualTo("검정");
        assertThat(result.getValue().getDescription()).containsExactly("검정", "가죽", "지갑", "소형", "로고");
    }

    @DisplayName("requestAutoFill은 바로 돌아오고 응답이 오면 비동기로 반영한다")
    @Test
    void requestAutoFillIsAsync() {
        match.enqueue(json(200, OK_BODY));

        client.requestAutoFill(EVENT);

        verify(autoFillService, timeout(5_000)).apply(eq(7L), any(AiGeneratedColumnDto.class));
    }

    @DisplayName("404(GPT api failed)는 반영하지 않고 예외도 밖으로 나오지 않는다")
    @Test
    void notFoundIsSwallowed() {
        match.enqueue(json(404, "{\"message\":\"GPT api failed\"}"));
        assertFailureSwallowed();
    }

    @DisplayName("500은 반영하지 않고 예외도 밖으로 나오지 않는다")
    @Test
    void serverErrorIsSwallowed() {
        match.enqueue(json(500, "{\"message\":\"boom\"}"));
        assertFailureSwallowed();
    }

    @DisplayName("깨진 JSON은 반영하지 않고 예외도 밖으로 나오지 않는다")
    @Test
    void brokenJsonIsSwallowed() {
        match.enqueue(json(200, "{\"message\":\"success\",\"result\":{\"category\":"));
        assertFailureSwallowed();
    }

    @DisplayName("result가 null이면 반영하지 않는다")
    @Test
    void nullResultIsSkipped() {
        match.enqueue(json(200, "{\"message\":\"success\",\"result\":null}"));
        assertFailureSwallowed();
    }

    @DisplayName("본문이 없는 200은 반영하지 않는다")
    @Test
    void emptyBodyIsSkipped() {
        match.enqueue(new MockResponse.Builder().code(200).build());
        assertFailureSwallowed();
    }

    @DisplayName("설정한 시간보다 늦은 응답은 반영하지 않는다")
    @Test
    void slowResponseTimesOut() {
        match.enqueue(new MockResponse.Builder().code(200).setHeader("Content-Type", "application/json")
                .body(OK_BODY).headersDelay(2, TimeUnit.SECONDS).build());

        long start = System.currentTimeMillis();
        assertFailureSwallowed();
        assertThat(System.currentTimeMillis() - start).isLessThan(1_900);
    }

    @DisplayName("연결할 수 없으면 반영하지 않는다")
    @Test
    void connectionRefusedIsSwallowed() throws IOException {
        String deadUrl = match.url("/").toString();
        match.close();
        client = clientFor(deadUrl);

        assertFailureSwallowed();
    }

    private void assertFailureSwallowed() {
        assertThatCode(() -> client.autoFill(EVENT).block(Duration.ofSeconds(5))).doesNotThrowAnyException();
        verify(autoFillService, never()).apply(any(), any());
    }
}
