package com.findear.main.board.query.service;

import com.findear.main.board.command.repository.Lost112ScrapRepository;
import com.findear.main.board.command.repository.ReturnLogRepository;
import com.findear.main.board.command.repository.ScrapRepository;
import com.findear.main.board.query.repository.AcquiredBoardQueryRepository;
import com.findear.main.common.config.WebConfig;
import com.findear.main.member.query.service.MemberQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * main -> batch 호출 경로 검증 (K-01). batch 계약은 docs/restoration/07-api-contracts.md §2.
 * DB·batch 서버 없이 MockRestServiceServer로 요청 URI와 메서드만 확인한다.
 */
class AcquiredBoardQueryServiceLost112Test {

    private static final String BATCH_URL = "http://batch.test";

    private static final String LIST_RESPONSE = """
            {"status":200,"message":"조회 성공","result":[
              {"id":1,"atcId":"F2024","depPlace":"강남경찰서","fdFilePathImg":"http://img/1.jpg",
               "fdPrdtNm":"검정 지갑","fdSbjt":"검정 지갑 습득","clrNm":"검정","fdYmd":"2026-09-01",
               "prdtClNm":"지갑","mainPrdtClNm":"지갑","subPrdtClNm":"기타"}
            ]}""";

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private AcquiredBoardQueryServiceImpl service;

    @BeforeEach
    void setup() {
        // 운영과 같은 구성의 batch 전용 RestTemplate (rootUri + TEMPLATE_AND_VALUES 인코딩)
        restTemplate = new WebConfig(new String[0]).batchRestTemplate(new RestTemplateBuilder(), BATCH_URL);
        server = MockRestServiceServer.bindTo(restTemplate).build();
        service = new AcquiredBoardQueryServiceImpl(
                Mockito.mock(AcquiredBoardQueryRepository.class),
                Mockito.mock(ReturnLogRepository.class),
                restTemplate,
                Mockito.mock(Lost112ScrapRepository.class),
                Mockito.mock(MemberQueryService.class),
                Mockito.mock(ScrapRepository.class));
    }

    @DisplayName("Lost112 목록은 {batch}/search?page&size 로 GET 요청한다")
    @Test
    void listUsesSearchPath() {
        server.expect(requestTo(BATCH_URL + "/search?page=2&size=10"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(LIST_RESPONSE, MediaType.APPLICATION_JSON));

        List<?> result = service.findAllInLost112(null, null, null, null, 2, 10);

        assertThat(result).hasSize(1);
        server.verify();
    }

    @DisplayName("Lost112 목록: 한글 category·keyword는 퍼센트 인코딩되고 기간 파라미터가 붙는다")
    @Test
    void listEncodesKoreanParams() {
        server.expect(requestTo(BATCH_URL + "/search?page=1&size=5"
                        + "&category=%EC%A7%80%EA%B0%91"                       // 지갑
                        + "&startDate=2026-01-01&endDate=2026-02-01"
                        + "&keyword=%EA%B2%80%EC%A0%95%20%EC%A7%80%EA%B0%91")) // 검정 지갑
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(LIST_RESPONSE, MediaType.APPLICATION_JSON));

        List<?> result = service.findAllInLost112("지갑", "2026-01-01", "2026-02-01", "검정 지갑", 1, 5);

        assertThat(result).hasSize(1);
        server.verify();
    }

    @DisplayName("Lost112 목록: keyword의 + & = 는 퍼센트 인코딩돼 batch가 a+b&c=d 그대로 받는다 (K-01), 한글은 UTF-8로 한 번만")
    @Test
    void listEncodesReservedCharactersInQueryValues() {
        server.expect(requestTo(BATCH_URL + "/search?page=1&size=10"
                        + "&category=%EC%A7%80%EA%B0%91"                  // 지갑
                        + "&keyword=a%2Bb%26c%3Dd"))                      // a+b&c=d
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(LIST_RESPONSE, MediaType.APPLICATION_JSON));

        service.findAllInLost112("지갑", null, null, "a+b&c=d", 1, 10);

        server.verify();
    }

    @DisplayName("Lost112 목록: 이미 %가 든 값도 한 번 더 인코딩된다 (값은 사용자 입력 그대로 전달)")
    @Test
    void listEncodesPercentLiterally() {
        server.expect(requestTo(BATCH_URL + "/search?page=1&size=10&keyword=100%25"))
                .andRespond(withSuccess(LIST_RESPONSE, MediaType.APPLICATION_JSON));

        service.findAllInLost112(null, null, null, "100%", 1, 10);

        server.verify();
    }

    @DisplayName("Lost112 목록: 시작일만 주면 종료일은 오늘로 채운다")
    @Test
    void listFillsEndDateWithToday() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        server.expect(requestTo(BATCH_URL + "/search?page=1&size=10&startDate=2026-01-01&endDate=" + today))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(LIST_RESPONSE, MediaType.APPLICATION_JSON));

        service.findAllInLost112(null, "2026-01-01", null, null, 1, 10);

        server.verify();
    }

    @DisplayName("Lost112 총 페이지 수는 {batch}/search/total 로 GET 요청해 올림 계산한다")
    @Test
    void totalUsesSearchTotalPath() {
        server.expect(requestTo(BATCH_URL + "/search/total"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":200,\"message\":\"조회 성공\",\"result\":25}",
                        MediaType.APPLICATION_JSON));

        Integer totalPage = service.getLost112TotalPageNum(10);

        assertThat(totalPage).isEqualTo(3);
        server.verify();
    }

    @DisplayName("Lost112 총 개수가 0이면 총 페이지 수는 1")
    @Test
    void totalPageIsAtLeastOne() {
        server.expect(requestTo(BATCH_URL + "/search/total"))
                .andRespond(withSuccess("{\"status\":200,\"message\":\"조회 성공\",\"result\":0}",
                        MediaType.APPLICATION_JSON));

        assertThat(service.getLost112TotalPageNum(10)).isEqualTo(1);
    }
}
