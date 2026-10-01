package com.findear.batch.common;

import com.findear.batch.common.exception.CommonControllerAdvice;
import com.findear.batch.common.job.BatchJobExceptionAdvice;
import com.findear.batch.ours.domain.FindearMatchingLog;
import com.findear.batch.police.exception.Lost112ExceptionAdvice;
import com.findear.batch.support.IntegrationTestBase;
import com.findear.batch.support.MatchMock;
import com.findear.batch.support.MatchResponses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.method.ControllerAdviceBean;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriTemplateHandler;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 공통 오류 응답 (R-36, D-51): 모든 실패는 {@code {"status", "message"}} JSON이고 status는 HTTP 상태와 같다.
 * 없는 분실물 404, 잘못된 입력 400, match 호출 실패 502, 없는 경로 404·메서드 405, 그 밖의 예외 500(고정 문구).
 */
class ErrorResponseTest extends IntegrationTestBase {

    private static final String LOST_BODY = "{\"lostBoardId\":\"1\",\"productName\":\"지갑\",\"color\":\"검정\",\"categoryName\":\"지갑\","
            + "\"description\":\"d\",\"lostAt\":\"%s\",\"xpos\":\"1.0\",\"ypos\":\"1.0\"}";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ApplicationContext context;
    @Autowired
    ElasticsearchOperations operations;
    @Autowired
    @Qualifier("matchRestTemplate")
    RestTemplate matchRestTemplate;

    /** 공통 실패 형식: JSON, status가 HTTP 상태와 같고 message가 문자열이며 Spring 기본 오류 JSON 필드나 result는 없다 */
    private static ResultActions failure(ResultActions actions, int status) throws Exception {
        return actions
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.result").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist());
    }

    private String lostBody(String lostAt) {
        return String.format(LOST_BODY, lostAt);
    }

    private void insertLost(long lostBoardId) {
        insertMember(1);
        insertBoard(10, 1, true, "지갑", "검정", "지갑", "d", LocalDateTime.now().minusDays(3));
        insertLostBoard(lostBoardId, 10, LocalDate.now().minusDays(3), 1f, 1f);
    }

    @DisplayName("없는 분실물의 매칭 목록은 404 (로그가 없는 분실물은 200 빈 목록)")
    @Test
    void missingLostBoardIs404() throws Exception {
        failure(mockMvc.perform(get("/findear/board/999").param("page", "1").param("size", "6")), 404)
                .andExpect(jsonPath("$.message").value("해당 분실물이 존재하지 않습니다."));
        failure(mockMvc.perform(get("/police/board/999").param("page", "1").param("size", "6")), 404)
                .andExpect(jsonPath("$.message").value("해당 분실물이 존재하지 않습니다."));

        insertLost(1);
        mockMvc.perform(get("/findear/board/1")).andExpect(status().isOk()).andExpect(jsonPath("$.result.totalCount").value(0));
        mockMvc.perform(get("/police/board/1")).andExpect(status().isOk()).andExpect(jsonPath("$.result.totalCount").value(0));
    }

    @DisplayName("잘못된 page·size, 필수 파라미터 누락, 타입 불일치, 날짜 형식 오류는 400")
    @Test
    void invalidQueryIs400() throws Exception {
        // page·size는 1 이상 (없는 분실물이어도 입력 검사가 먼저라 400)
        failure(mockMvc.perform(get("/findear/board/1").param("page", "0")), 400);
        failure(mockMvc.perform(get("/findear/board/1").param("size", "0")), 400);
        failure(mockMvc.perform(get("/findear/member/1").param("page", "0")), 400);
        failure(mockMvc.perform(get("/findear/member/1").param("size", "-1")), 400);
        failure(mockMvc.perform(get("/police/board/1").param("page", "-1")), 400);
        failure(mockMvc.perform(get("/police/board/1").param("size", "0")), 400);
        failure(mockMvc.perform(get("/police/member/1").param("page", "0")), 400);
        failure(mockMvc.perform(get("/search").param("page", "0").param("size", "10")), 400)
                .andExpect(jsonPath("$.message").value("page는 1 이상이어야 합니다."));
        failure(mockMvc.perform(get("/search").param("page", "1").param("size", "0")), 400)
                .andExpect(jsonPath("$.message").value("size는 1 이상이어야 합니다."));
        // 필수 파라미터 누락, 타입 불일치
        failure(mockMvc.perform(get("/search").param("page", "1")), 400);
        failure(mockMvc.perform(get("/search").param("page", "abc").param("size", "10")), 400);
        failure(mockMvc.perform(get("/findear/board/abc")), 400);
        // 날짜 형식
        failure(mockMvc.perform(get("/search").param("page", "1").param("size", "10").param("startDate", "2026/10/01")), 400)
                .andExpect(jsonPath("$.message").value(containsString("startDate")));
        failure(mockMvc.perform(get("/search").param("page", "1").param("size", "10").param("endDate", "내일")), 400)
                .andExpect(jsonPath("$.message").value(containsString("endDate")));
    }

    @DisplayName("분실물 매칭 요청: 깨진 JSON·빈 본문·잘못된 날짜·숫자가 아닌 ID·필수값 누락은 400이고 match는 호출하지 않는다")
    @Test
    void invalidMatchingBodyIs400() throws Exception {
        failure(mockMvc.perform(post("/findear/matching").contentType(MediaType.APPLICATION_JSON).content("{\"lostBoardId\":")), 400);
        failure(mockMvc.perform(post("/findear/matching").contentType(MediaType.APPLICATION_JSON)), 400);
        failure(mockMvc.perform(post("/findear/matching").contentType(MediaType.APPLICATION_JSON).content(lostBody("2026-09-20T10:30:00"))), 400)
                .andExpect(jsonPath("$.message").value(containsString("lostAt")));
        failure(mockMvc.perform(post("/findear/matching").contentType(MediaType.APPLICATION_JSON).content(lostBody("20260920"))), 400);
        failure(mockMvc.perform(post("/findear/matching").contentType(MediaType.APPLICATION_JSON)
                .content(lostBody("2026-09-20").replace("\"1\"", "\"abc\""))), 400)
                .andExpect(jsonPath("$.message").value(containsString("lostBoardId")));
        failure(mockMvc.perform(post("/findear/matching").contentType(MediaType.APPLICATION_JSON)
                .content("{\"lostBoardId\":\"1\",\"lostAt\":\"2026-09-20\"}")), 400)
                .andExpect(jsonPath("$.message").value(containsString("categoryName")));
        failure(mockMvc.perform(post("/findear/matching").contentType(MediaType.TEXT_PLAIN).content("x")), 415);

        failure(mockMvc.perform(post("/police/scrap").contentType(MediaType.APPLICATION_JSON).content("[")), 400);
        failure(mockMvc.perform(post("/police/scrap").contentType(MediaType.APPLICATION_JSON).content("{\"atcIdList\":[null]}")), 400);

        assertThat(MATCH.requests()).isEmpty();
    }

    @DisplayName("match가 오류(5xx)를 주면 502 고정 문구, match의 응답 내용은 노출하지 않는다")
    @Test
    void matchServerErrorIs502() throws Exception {
        MATCH.respondWith(request -> MatchMock.json(500, "{\"message\":\"match 내부 사정\"}"));

        failure(mockMvc.perform(post("/findear/matching").contentType(MediaType.APPLICATION_JSON).content(lostBody("2026-09-20"))), 502)
                .andExpect(jsonPath("$.message").value("매칭 서버 호출에 실패했습니다."))
                .andExpect(content().string(not(containsString("match 내부 사정"))));

        // findear 호출은 성공하고 lost112 호출만 실패해도 502
        MATCH.respondWith(request -> request.getUrl().encodedPath().equals("/matching/findear")
                ? MatchResponses.respond(request) : MatchMock.json(503, "{\"message\":\"x\"}"));
        failure(mockMvc.perform(post("/findear/matching").contentType(MediaType.APPLICATION_JSON).content(lostBody("2026-09-20"))), 502)
                .andExpect(jsonPath("$.message").value("매칭 서버 호출에 실패했습니다."));
    }

    @DisplayName("match에 연결할 수 없으면 502 고정 문구 (닫힌 로컬 포트로 흉내)")
    @Test
    void matchServerUnreachableIs502() throws Exception {
        UriTemplateHandler original = matchRestTemplate.getUriTemplateHandler();
        // 127.0.0.1:1 은 아무도 듣지 않는 로컬 포트라 곧바로 연결 거부된다 (외부 호출 아님)
        matchRestTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory("http://127.0.0.1:1"));
        try {
            failure(mockMvc.perform(post("/findear/matching").contentType(MediaType.APPLICATION_JSON).content(lostBody("2026-09-20"))), 502)
                    .andExpect(jsonPath("$.message").value("매칭 서버 호출에 실패했습니다."))
                    .andExpect(content().string(not(containsString("127.0.0.1"))));
        } finally {
            matchRestTemplate.setUriTemplateHandler(original);
        }
        assertThat(MATCH.requests()).isEmpty();
    }

    @DisplayName("없는 경로 404, 허용하지 않는 메서드 405 (같은 형식). 삭제한 test 엔드포인트는 404")
    @Test
    void unknownPathAndMethod() throws Exception {
        failure(mockMvc.perform(get("/no-such-path")), 404);
        failure(mockMvc.perform(get("/findear/test-api")), 404);
        failure(mockMvc.perform(get("/police/test-api")), 404);
        failure(mockMvc.perform(get("/search/test")), 404);
        failure(mockMvc.perform(get("/findear/matching")), 405);
        failure(mockMvc.perform(get("/findear/matching/batch")), 405);
        failure(mockMvc.perform(delete("/search/total")), 405);
    }

    @DisplayName("처리되지 않은 예외(ES 인덱스 없음)는 500 고정 문구, 원인은 응답에 없다")
    @Test
    void unexpectedExceptionIs500() throws Exception {
        insertLost(1);
        IndexOperations indexOps = operations.indexOps(FindearMatchingLog.class);
        indexOps.delete();
        try {
            failure(mockMvc.perform(get("/findear/board/1").param("page", "1").param("size", "6")), 500)
                    .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."))
                    .andExpect(content().string(not(containsString("index"))))
                    .andExpect(content().string(not(containsString(FindearMatchingLog.INDEX))));
        } finally {
            if (indexOps.exists()) {
                indexOps.delete();
            }
            indexOps.createWithMapping();
        }
    }

    @DisplayName("Lost112 키가 없으면 여전히 503 {status, message}")
    @Test
    void lost112NotConfiguredIs503() throws Exception {
        failure(mockMvc.perform(post("/search/save")), 503)
                .andExpect(jsonPath("$.message").value("Lost112 API 키가 설정되지 않았습니다."));
    }

    @DisplayName("구체적인 advice(Lost112 503·502, 잡 실행 중 409)가 공통 advice보다 먼저 적용되는 순서")
    @Test
    void specificAdvicesComeFirst() {
        List<Class<?>> order = ControllerAdviceBean.findAnnotatedBeans(context).stream()
                .map(ControllerAdviceBean::getBeanType)
                .toList();

        assertThat(order).contains(Lost112ExceptionAdvice.class, BatchJobExceptionAdvice.class, CommonControllerAdvice.class);
        assertThat(order.indexOf(Lost112ExceptionAdvice.class)).isLessThan(order.indexOf(CommonControllerAdvice.class));
        assertThat(order.indexOf(BatchJobExceptionAdvice.class)).isLessThan(order.indexOf(CommonControllerAdvice.class));
        // 공통 advice는 모든 예외를 받으므로 구체 advice를 가리지 않는지 확인용: 각 구체 advice는 자기 예외 핸들러를 가진다
        assertThat(Lost112ExceptionAdvice.class.getDeclaredMethods())
                .anyMatch(m -> m.isAnnotationPresent(ExceptionHandler.class));
        assertThat(BatchJobExceptionAdvice.class.getDeclaredMethods())
                .anyMatch(m -> m.isAnnotationPresent(ExceptionHandler.class));
    }
}
