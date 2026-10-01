package com.findear.main.common.config;

import com.findear.main.common.utils.query.LocationController;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.client.HttpClientAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.http.client.reactive.ClientHttpConnectorAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * HTTP 클라이언트 시간 제한 검증 (R-50). main의 application.yml(spring.http.client.*, spring.http.reactiveclient.*)을 읽는
 * Boot 자동 구성 컨텍스트에서 Builder를 얻는다. 외부 서버는 호출하지 않고 mock 서버(mockwebserver3)와 라우팅되지 않는 주소만 쓴다.
 * - 공용 RestTemplate(WebConfig): 읽기 제한 10초
 * - VWorld 전용 RestTemplate(LocationController): 빌더에서 정한 3s/5s가 전역 값보다 우선
 * - WebClient: 연결 제한 3초
 */
class HttpClientTimeoutTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withConfiguration(AutoConfigurations.of(HttpClientAutoConfiguration.class, RestTemplateAutoConfiguration.class,
                    ClientHttpConnectorAutoConfiguration.class, WebClientAutoConfiguration.class));

    private MockWebServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void stopServer() throws IOException {
        server.close();
    }

    private long elapsedMillisUntilReadTimeout(RestTemplate restTemplate) {
        server.enqueue(new MockResponse.Builder().headersDelay(60, TimeUnit.SECONDS).build());
        long start = System.nanoTime();
        assertThatThrownBy(() -> restTemplate.getForObject(server.url("/slow").uri(), String.class))
                .isInstanceOf(ResourceAccessException.class);
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    @DisplayName("공용 RestTemplate: 응답이 없으면 읽기 제한(10초)에서 실패한다")
    @Test
    void sharedRestTemplateReadTimeout() {
        runner.run(context -> {
            RestTemplate restTemplate = new WebConfig(new String[0]).restTemplate(context.getBean(RestTemplateBuilder.class));

            long elapsed = elapsedMillisUntilReadTimeout(restTemplate);

            assertThat(elapsed).isBetween(9_000L, 13_000L);
        });
    }

    @DisplayName("VWorld 전용 RestTemplate: 전역 값(10초)이 아니라 자기 읽기 제한(5초)을 유지한다")
    @Test
    void vworldRestTemplateKeepsOwnTimeouts() {
        runner.run(context -> {
            LocationController controller = new LocationController("test-key", server.url("/").toString(),
                    context.getBean(RestTemplateBuilder.class));
            RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(controller, "restTemplate");

            long elapsed = elapsedMillisUntilReadTimeout(restTemplate);

            assertThat(elapsed).isBetween(4_000L, 8_000L);
        });
    }

    @DisplayName("batch 전용 RestTemplate: Boot 자동 구성 빌더로 만들어도 rootUri가 붙고 변수 값은 엄격하게 한 번만 인코딩된다")
    @Test
    void batchRestTemplateExpandsRootUriAndEncodesValues() {
        runner.run(context -> {
            RestTemplate restTemplate = new WebConfig(new String[0])
                    .batchRestTemplate(context.getBean(RestTemplateBuilder.class), "http://batch.test:8082");

            String uri = restTemplate.getUriTemplateHandler()
                    .expand("/search?page={page}&keyword={keyword}", 1, "a+b&c=d 100% 한글").toString();

            assertThat(uri).isEqualTo("http://batch.test:8082/search?page=1&keyword=a%2Bb%26c%3Dd%20100%25%20%ED%95%9C%EA%B8%80");
        });
    }

    @DisplayName("WebClient: 연결할 수 없는 주소에는 연결 제한(3초)에서 실패한다 (기본 30초 아님)")
    @Test
    void webClientConnectTimeout() {
        runner.run(context -> {
            // RFC 5737 문서화용 주소: 라우팅되지 않아 SYN에 응답이 없다. 환경에 따라 즉시 실패(네트워크 없음 등)하면 검증할 수 없으므로 건너뛴다
            WebClient client = context.getBean(WebClient.Builder.class).baseUrl("http://192.0.2.1:81").build();

            long start = System.nanoTime();
            Throwable error = null;
            try {
                client.get().uri("/").retrieve().bodyToMono(String.class).block(Duration.ofSeconds(15));
            } catch (Throwable e) {
                error = e;
            }
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            assertThat(error).isNotNull();
            Assumptions.assumeTrue(elapsed > 1_000, "연결 시도가 즉시 실패해 연결 제한을 검증할 수 없음: " + error);
            assertThat(elapsed).isBetween(2_500L, 6_000L);
        });
    }
}
