package com.findear.main.common.utils.query;

import com.findear.main.common.exception.CommonControllerAdvice;
import com.findear.main.common.exception.ExternalServiceExceptionAdvice;
import com.findear.main.common.exception.ExternalServiceNotConfiguredException;
import com.findear.main.member.common.exception.MemberControllerAdvice;
import okhttp3.HttpUrl;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * VWorld 프록시 계약 테스트. VWorld는 호출하지 않고 mock 서버로 공식 문서(검색 API 2.0, 주소→좌표 변환 API 2.0)의
 * 요청 파라미터·응답 구조·오류 응답 구조를 재현한다. 응답 JSON 예시는 문서의 응답 필드 설명에 맞춰 만든 것이다
 * (문서에 완전한 JSON 예시가 없다).
 */
class LocationControllerTest {

    // 검색 API 2.0 PLACE 응답 (record/page/result.items[].{id,title,category,address,point})
    private static final String SEARCH_OK = """
            {"response":{"service":{"name":"search","version":"2.0","operation":"search","time":"12(ms)"},
            "status":"OK","record":{"total":"1","current":"1"},"page":{"total":"1","current":"1","size":"5"},
            "result":{"crs":"EPSG:4326","type":"place","items":[{"id":"1","title":"서울역","category":"철도역",
            "address":{"road":"서울특별시 용산구 한강대로 405","parcel":"서울특별시 용산구 동자동 43-205"},
            "point":{"x":"126.9723","y":"37.5559"}}]}}}""";
    private static final String NOT_FOUND = """
            {"response":{"service":{"name":"search","version":"2.0","operation":"search","time":"3(ms)"},
            "status":"NOT_FOUND"}}""";
    // 오류 응답: response.status=ERROR, response.error.{level,code,text}
    private static final String ERROR_INVALID_KEY = """
            {"response":{"error":{"level":"2","code":"INVALID_KEY","text":"등록되지 않은 인증키입니다."},"status":"ERROR"}}""";
    // 주소→좌표 변환 API 2.0 (simple=false: input, refined, result.point)
    private static final String ADDRESS_OK = """
            {"response":{"service":{"name":"address","version":"2.0","operation":"getcoord","time":"20(ms)"},
            "status":"OK","input":{"type":"road","address":"서울특별시 용산구 한강대로 405"},
            "refined":{"text":"서울특별시 용산구 한강대로 405 (동자동)","structure":{"level0":"대한민국","level1":"서울특별시"}},
            "result":{"crs":"EPSG:4326","point":{"x":"126.9723","y":"37.5559"}}}}""";

    private MockWebServer vworld;

    @BeforeEach
    void startMock() throws IOException {
        vworld = new MockWebServer();
        vworld.start();
    }

    @AfterEach
    void stopMock() throws IOException {
        vworld.close();
    }

    // 세 advice를 일부러 우선순위와 반대 순서로 등록해도 ExternalServiceExceptionAdvice가 먼저 적용되는지 확인한다
    private MockMvc mvc(String apiKey, String baseUrl) {
        LocationController controller = new LocationController(apiKey, baseUrl, new RestTemplateBuilder());
        return MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new MemberControllerAdvice(), new CommonControllerAdvice(),
                        new ExternalServiceExceptionAdvice())
                // WebConfig와 같이 Accept가 없으면 JSON (jackson-dataformat-xml이 있어 기본은 XML이 먼저 선택된다)
                .setContentNegotiationManager(new ContentNegotiationManager(
                        new HeaderContentNegotiationStrategy(), new FixedContentNegotiationStrategy(MediaType.APPLICATION_JSON)))
                .build();
    }

    private MockMvc mvc() {
        return mvc("test-key+/=", vworld.url("/").toString());
    }

    private static MockResponse json(String body) {
        return new MockResponse.Builder().setHeader("Content-Type", "application/json;charset=UTF-8").body(body).build();
    }

    @Test
    void search_요청_파라미터가_공식_문서_필수값과_같고_한글이_한번만_인코딩된다() throws Exception {
        vworld.enqueue(json(SEARCH_OK));

        mvc().perform(get("/location/search").param("query", "서울역 & 1+1").param("page", "2").param("size", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        RecordedRequest req = vworld.takeRequest(1, TimeUnit.SECONDS);
        assertThat(req).isNotNull();
        assertThat(req.getMethod()).isEqualTo("GET");
        HttpUrl url = req.getUrl();
        assertThat(url.encodedPath()).isEqualTo("/req/search");
        assertThat(url.queryParameter("service")).isEqualTo("search");
        assertThat(url.queryParameter("request")).isEqualTo("search");
        assertThat(url.queryParameter("version")).isEqualTo("2.0");
        assertThat(url.queryParameter("type")).isEqualTo("PLACE");
        assertThat(url.queryParameter("format")).isEqualTo("json");
        assertThat(url.queryParameter("errorformat")).isEqualTo("json");
        assertThat(url.queryParameter("crs")).isEqualToIgnoringCase("EPSG:4326");
        assertThat(url.queryParameter("size")).isEqualTo("5");
        assertThat(url.queryParameter("page")).isEqualTo("2");
        assertThat(url.queryParameter("key")).isEqualTo("test-key+/=");
        // 디코드하면 원문이고, 이중 인코딩(%25)되지 않았다
        assertThat(url.queryParameter("query")).isEqualTo("서울역 & 1+1");
        assertThat(url.encodedQuery()).doesNotContain("%25");
        assertThat(url.encodedQuery()).contains("query=%EC%84%9C%EC%9A%B8%EC%97%AD%20%26%201%2B1");
    }

    @Test
    void search_size_page를_생략하면_문서_기본값_10_1을_보낸다() throws Exception {
        vworld.enqueue(json(SEARCH_OK));

        mvc().perform(get("/location/search").param("query", "서울역")).andExpect(status().isOk());

        HttpUrl url = vworld.takeRequest(1, TimeUnit.SECONDS).getUrl();
        assertThat(url.queryParameter("size")).isEqualTo("10");
        assertThat(url.queryParameter("page")).isEqualTo("1");
    }

    @Test
    void search_정상_응답은_본문을_그대로_돌려주고_UTF8_JSON이다() throws Exception {
        vworld.enqueue(json(SEARCH_OK));

        MvcResult result = mvc().perform(get("/location/search").param("query", "서울역"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.response.status").value("OK"))
                .andExpect(jsonPath("$.response.result.items[0].title").value("서울역"))
                .andReturn();
        assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(SEARCH_OK.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void search_NOT_FOUND는_본문_그대로_200() throws Exception {
        vworld.enqueue(json(NOT_FOUND));

        mvc().perform(get("/location/search").param("query", "없는곳"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value("NOT_FOUND"));
    }

    @Test
    void search_VWorld_ERROR_응답도_본문_그대로_200() throws Exception {
        vworld.enqueue(json(ERROR_INVALID_KEY));

        mvc().perform(get("/location/search").param("query", "서울역"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value("ERROR"))
                .andExpect(jsonPath("$.response.error.code").value("INVALID_KEY"))
                .andExpect(jsonPath("$.response.error.level").value("2"));
    }

    @Test
    void search_VWorld가_5xx면_502와_공통_실패_형식_그리고_키는_노출하지_않는다() throws Exception {
        vworld.enqueue(new MockResponse.Builder().code(500).body("oops").build());

        mvc().perform(get("/location/search").param("query", "서울역"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value("VWorld 호출에 실패했습니다"))
                .andExpect(content().string(not(containsString("test-key"))));
    }

    @Test
    void search_VWorld에_연결할_수_없으면_502() throws Exception {
        String deadUrl = vworld.url("/").toString();
        vworld.close();

        mvc("test-key", deadUrl).perform(get("/location/search").param("query", "서울역"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value("VWorld 호출에 실패했습니다"));
    }

    @Test
    void search_응답이_없어_읽기_타임아웃이_나면_502() throws Exception {
        vworld.enqueue(new MockResponse.Builder().headersDelay(30, TimeUnit.SECONDS).build());

        long start = System.currentTimeMillis();
        mvc().perform(get("/location/search").param("query", "서울역"))
                .andExpect(status().isBadGateway());
        assertThat(System.currentTimeMillis() - start).isLessThan(8_000);
    }

    @Test
    void address_요청_파라미터가_공식_문서_필수값과_같고_한글이_한번만_인코딩된다() throws Exception {
        vworld.enqueue(json(ADDRESS_OK));

        mvc().perform(get("/location/address").param("address", "서울특별시 용산구 한강대로 405")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.response.status").value("OK"))
                .andExpect(jsonPath("$.response.result.point.x").value("126.9723"))
                .andExpect(jsonPath("$.response.refined.text").value("서울특별시 용산구 한강대로 405 (동자동)"));

        HttpUrl url = vworld.takeRequest(1, TimeUnit.SECONDS).getUrl();
        assertThat(url.encodedPath()).isEqualTo("/req/address");
        assertThat(url.queryParameter("service")).isEqualTo("address");
        assertThat(url.queryParameter("request")).isEqualToIgnoringCase("getcoord");
        assertThat(url.queryParameter("version")).isEqualTo("2.0");
        assertThat(url.queryParameter("type")).isEqualToIgnoringCase("road");
        assertThat(url.queryParameter("format")).isEqualTo("json");
        assertThat(url.queryParameter("errorformat")).isEqualTo("json");
        assertThat(url.queryParameter("refine")).isEqualTo("true");
        assertThat(url.queryParameter("simple")).isEqualTo("false");
        assertThat(url.queryParameter("crs")).isEqualToIgnoringCase("EPSG:4326");
        assertThat(url.queryParameter("key")).isEqualTo("test-key+/=");
        assertThat(url.queryParameter("address")).isEqualTo("서울특별시 용산구 한강대로 405");
        assertThat(url.encodedQuery()).doesNotContain("%25");
    }

    @Test
    void address_NOT_FOUND와_ERROR도_본문_그대로_200() throws Exception {
        vworld.enqueue(json(NOT_FOUND.replace("\"search\"", "\"address\"")));
        vworld.enqueue(json(ERROR_INVALID_KEY));

        mvc().perform(get("/location/address").param("address", "없는 주소"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.status").value("NOT_FOUND"));
        mvc().perform(get("/location/address").param("address", "서울특별시 용산구 한강대로 405"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.error.code").value("INVALID_KEY"));
    }

    @Test
    void 키가_비어_있으면_503이고_VWorld에_요청을_보내지_않는다() throws Exception {
        for (String key : new String[]{"", "  "}) {
            mvc(key, vworld.url("/").toString()).perform(get("/location/search").param("query", "서울역"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.status").value(503))
                    .andExpect(jsonPath("$.message").value("VWorld가 설정되지 않았습니다: VWORLD_API_KEY"));
            mvc(key, vworld.url("/").toString()).perform(get("/location/address").param("address", "서울특별시 용산구 한강대로 405"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.message").value("VWorld가 설정되지 않았습니다: VWORLD_API_KEY"));
        }
        assertThat(vworld.getRequestCount()).isZero();
    }

    @Test
    void query_address가_비어_있거나_size_page가_범위를_벗어나면_400이고_VWorld에_요청을_보내지_않는다() throws Exception {
        MockMvc mvc = mvc();
        mvc.perform(get("/location/search")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/location/search").param("query", "  ")).andExpect(status().isBadRequest());
        mvc.perform(get("/location/search").param("query", "a").param("size", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/location/search").param("query", "a").param("size", "1001")).andExpect(status().isBadRequest());
        mvc.perform(get("/location/search").param("query", "a").param("page", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/location/address")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/location/address").param("address", "")).andExpect(status().isBadRequest());
        assertThat(vworld.getRequestCount()).isZero();
    }

    @Test
    void 경계값_size_1과_1000은_허용한다() throws Exception {
        vworld.enqueue(json(SEARCH_OK));
        vworld.enqueue(json(SEARCH_OK));

        mvc().perform(get("/location/search").param("query", "a").param("size", "1")).andExpect(status().isOk());
        mvc().perform(get("/location/search").param("query", "a").param("size", "1000")).andExpect(status().isOk());
        assertThat(vworld.getRequestCount()).isEqualTo(2);
    }

    @Test
    void 미설정_예외_메시지는_받침에_따라_조사가_바뀐다() {
        assertThat(new ExternalServiceNotConfiguredException("VWorld", "VWORLD_API_KEY").getMessage())
                .isEqualTo("VWorld가 설정되지 않았습니다: VWORLD_API_KEY");
        assertThat(new ExternalServiceNotConfiguredException("Naver 로그인", "NAVER_CLIENT_ID", "NAVER_CLIENT_SECRET").getMessage())
                .isEqualTo("Naver 로그인이 설정되지 않았습니다: NAVER_CLIENT_ID, NAVER_CLIENT_SECRET");
    }
}
