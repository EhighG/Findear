package com.findear.main.board.query.service;

import com.findear.main.board.command.repository.Lost112ScrapRepository;
import com.findear.main.board.command.repository.ReturnLogRepository;
import com.findear.main.board.command.repository.ScrapRepository;
import com.findear.main.board.query.repository.AcquiredBoardQueryRepository;
import com.findear.main.board.query.repository.LostBoardQueryRepository;
import com.findear.main.common.config.WebConfig;
import com.findear.main.matching.service.MatchingServiceImpl;
import com.findear.main.member.query.service.MemberQueryService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * main → batch 호출의 HTTP 클라이언트 지표(http.client.requests) uri 태그 검증 (R-50).
 * 완성된 URL 문자열이나 URI 객체로 호출하면 uri 태그에 실제 id·검색어가 들어가 시계열이 호출마다 늘어난다.
 * batch 전용 RestTemplate에 URI 템플릿 + 변수로 호출하면 태그는 고정 템플릿이어야 한다.
 */
class BatchCallMetricsTest {

    private static final String BATCH_URL = "http://batch.test";

    private MeterRegistry meterRegistry;
    private RestTemplate batchRestTemplate;
    private MockRestServiceServer server;

    @BeforeEach
    void setup() {
        meterRegistry = new SimpleMeterRegistry();
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        observationRegistry.observationConfig().observationHandler(new DefaultMeterObservationHandler(meterRegistry));
        batchRestTemplate = new WebConfig(new String[0]).batchRestTemplate(new RestTemplateBuilder(), BATCH_URL);
        batchRestTemplate.setObservationRegistry(observationRegistry);
        server = MockRestServiceServer.bindTo(batchRestTemplate).ignoreExpectOrder(true).build();
    }

    private Set<String> uriTags() {
        return meterRegistry.find("http.client.requests").timers().stream()
                .map(Timer::getId).map(id -> id.getTag("uri")).collect(Collectors.toSet());
    }

    private void respond(String body, int times) {
        for (int i = 0; i < times; i++) {
            server.expect(anything()).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        }
    }

    @DisplayName("Lost112 목록: 실제 keyword가 아니라 고정 템플릿이 uri 태그가 된다 (파라미터 조합마다 템플릿만 달라진다)")
    @Test
    void lost112ListUriTagIsTemplate() {
        respond("{\"status\":200,\"message\":\"ok\",\"result\":[]}", 2);
        AcquiredBoardQueryServiceImpl service = new AcquiredBoardQueryServiceImpl(
                Mockito.mock(AcquiredBoardQueryRepository.class), Mockito.mock(ReturnLogRepository.class), batchRestTemplate,
                Mockito.mock(Lost112ScrapRepository.class), Mockito.mock(MemberQueryService.class), Mockito.mock(ScrapRepository.class));

        service.findAllInLost112(null, null, null, "a+b&secret-keyword", 3, 7);
        service.findAllInLost112(null, null, null, "다른 검색어", 4, 7);

        assertThat(uriTags()).containsExactly("/search?page={page}&size={size}&keyword={keyword}");
        assertThat(uriTags()).noneMatch(tag -> tag.contains("secret") || tag.contains("a+b") || tag.contains("검색어"));
    }

    @DisplayName("Lost112 총 개수·스크랩 목록도 같은 빈의 고정 경로를 쓴다")
    @Test
    void totalAndScrapUriTags() {
        server.expect(anything()).andRespond(withSuccess("{\"status\":200,\"message\":\"ok\",\"result\":25}", MediaType.APPLICATION_JSON));
        server.expect(anything()).andRespond(withSuccess("{\"status\":200,\"message\":\"ok\",\"result\":[]}", MediaType.APPLICATION_JSON));
        Lost112ScrapRepository lost112ScrapRepository = Mockito.mock(Lost112ScrapRepository.class);
        ScrapRepository scrapRepository = Mockito.mock(ScrapRepository.class);
        Mockito.when(lost112ScrapRepository.findAllByMember(Mockito.any())).thenReturn(List.of());
        Mockito.when(scrapRepository.findAllByMember(Mockito.any())).thenReturn(List.of());
        AcquiredBoardQueryServiceImpl service = new AcquiredBoardQueryServiceImpl(
                Mockito.mock(AcquiredBoardQueryRepository.class), Mockito.mock(ReturnLogRepository.class), batchRestTemplate,
                lost112ScrapRepository, Mockito.mock(MemberQueryService.class), scrapRepository);

        service.getLost112TotalPageNum(10);
        service.findScrapList(1L);

        assertThat(uriTags()).containsExactlyInAnyOrder("/search/total", "/police/scrap");
    }

    @DisplayName("매칭 목록: 회원·분실물 id가 아니라 고정 템플릿이 uri 태그가 된다")
    @Test
    void matchingUriTagIsTemplate() {
        respond("{\"status\":200,\"message\":\"ok\",\"result\":{\"matchingList\":[],\"totalCount\":0}}", 2);
        MatchingServiceImpl service = new MatchingServiceImpl(batchRestTemplate,
                Mockito.mock(LostBoardQueryRepository.class), Mockito.mock(AcquiredBoardQueryRepository.class));

        service.getFindearBestMatchings(123456L, 1, 6);
        service.getFindearMatchingList(987654L, 2, 6);

        assertThat(uriTags()).containsExactly("/{src}/{param}/{id}?page={page}&size={size}");
        assertThat(uriTags()).noneMatch(tag -> tag.contains("123456") || tag.contains("987654"));
    }

    @Configuration
    static class WiringConfig {
        @Bean
        @Primary
        RestTemplate restTemplate() {
            return new RestTemplate();
        }

        @Bean(WebConfig.BATCH_REST_TEMPLATE)
        RestTemplate batchRestTemplate() {
            return new WebConfig(new String[0]).batchRestTemplate(new RestTemplateBuilder(), BATCH_URL);
        }

        @Bean
        AcquiredBoardQueryRepository acquiredBoardQueryRepository() {
            return Mockito.mock(AcquiredBoardQueryRepository.class);
        }

        @Bean
        LostBoardQueryRepository lostBoardQueryRepository() {
            return Mockito.mock(LostBoardQueryRepository.class);
        }

        @Bean
        ReturnLogRepository returnLogRepository() {
            return Mockito.mock(ReturnLogRepository.class);
        }

        @Bean
        Lost112ScrapRepository lost112ScrapRepository() {
            return Mockito.mock(Lost112ScrapRepository.class);
        }

        @Bean
        ScrapRepository scrapRepository() {
            return Mockito.mock(ScrapRepository.class);
        }

        @Bean
        MemberQueryService memberQueryService() {
            return Mockito.mock(MemberQueryService.class);
        }

        @Bean
        AcquiredBoardQueryServiceImpl acquiredBoardQueryService(AcquiredBoardQueryRepository a, ReturnLogRepository r,
                                                                @org.springframework.beans.factory.annotation.Qualifier(WebConfig.BATCH_REST_TEMPLATE) RestTemplate t,
                                                                Lost112ScrapRepository l, MemberQueryService m, ScrapRepository s) {
            return new AcquiredBoardQueryServiceImpl(a, r, t, l, m, s);
        }
    }

    @DisplayName("스프링 주입: RestTemplate 빈이 둘이어도 두 서비스는 @Primary 공용이 아니라 batch 전용을 받는다")
    @Test
    void servicesGetBatchRestTemplate() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(WiringConfig.class)) {
            context.registerBean(MatchingServiceImpl.class);
            RestTemplate batch = context.getBean(WebConfig.BATCH_REST_TEMPLATE, RestTemplate.class);

            assertThat(ReflectionTestUtils.getField(context.getBean(AcquiredBoardQueryServiceImpl.class), "batchRestTemplate")).isSameAs(batch);
            assertThat(ReflectionTestUtils.getField(context.getBean(MatchingServiceImpl.class), "batchRestTemplate")).isSameAs(batch);
        }
    }
}
