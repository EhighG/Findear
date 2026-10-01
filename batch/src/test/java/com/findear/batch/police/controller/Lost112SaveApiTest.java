package com.findear.batch.police.controller;

import com.findear.batch.police.client.Lost112Properties;
import com.findear.batch.support.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static com.findear.batch.support.Lost112Fixtures.xml;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 수동 실행 API {@code POST /search/save} (07 §3): 키 없음 503, 수집 성공 200 + 요약, 전부 실패 502 (D-49).
 * Lost112는 mock 서버다 (D-38).
 */
class Lost112SaveApiTest extends IntegrationTestBase {

    private static final String POLICE_PATH = "/LosfundInfoInqireService/getLosfundInfoAccToClAreaPd";
    private static final String KEY = "ab+c/d==";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    Lost112Properties properties;

    private String originalKey;
    private int originalPageSize;

    @BeforeEach
    void saveProperties() {
        originalKey = properties.getServiceKey();
        originalPageSize = properties.getPageSize();
        properties.setPageSize(2);
    }

    @AfterEach
    void restoreProperties() {
        properties.setServiceKey(originalKey);
        properties.setPageSize(originalPageSize);
    }

    private void respondWithPages() {
        LOST112.respondWith(request -> {
            String prefix = request.getUrl().encodedPath().equals(POLICE_PATH) ? "police-page" : "portal-page";
            String pageNo = request.getUrl().queryParameter("pageNo");
            return "1".equals(pageNo) || "2".equals(pageNo) ? xml(prefix + pageNo + ".xml") : xml("no-data.xml");
        });
    }

    @DisplayName("키가 없으면 503 {status, message}이고 Lost112 요청은 0건")
    @Test
    void withoutKeyReturns503() throws Exception {
        properties.setServiceKey("");

        mockMvc.perform(post("/search/save"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("Lost112 API 키가 설정되지 않았습니다."))
                .andExpect(jsonPath("$.result").doesNotExist());

        assertThat(LOST112.requests()).isEmpty();
    }

    @DisplayName("mock 성공: 200 + 서비스별 요약, 목록 API는 습득일 최신순으로 문서를 돌려준다")
    @Test
    void successReturnsSummary() throws Exception {
        properties.setServiceKey(KEY);
        respondWithPages();

        mockMvc.perform(post("/search/save"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result.services.length()").value(2))
                .andExpect(jsonPath("$.result.services[0].service").value("POLICE"))
                .andExpect(jsonPath("$.result.services[0].pages").value(2))
                .andExpect(jsonPath("$.result.services[0].fetched").value(4))
                .andExpect(jsonPath("$.result.services[0].indexed").value(3))
                .andExpect(jsonPath("$.result.services[0].skipped").value(1))
                .andExpect(jsonPath("$.result.services[0].error").doesNotExist())
                .andExpect(jsonPath("$.result.services[1].service").value("PORTAL"))
                .andExpect(jsonPath("$.result.services[1].indexed").value(3))
                .andExpect(content().string(not(containsString("ab+c"))));

        assertThat(LOST112.requests()).hasSize(4);

        mockMvc.perform(get("/search/total")).andExpect(jsonPath("$.result").value(6));
        mockMvc.perform(get("/search").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(6))
                .andExpect(jsonPath("$.result[0].id").value("F2099010100000001"))
                .andExpect(jsonPath("$.result[0].atcId").value("F2099010100000001"))
                .andExpect(jsonPath("$.result[0].clrNm").value("검정"))
                .andExpect(jsonPath("$.result[0].fdYmd").value("2026-09-30"))
                .andExpect(jsonPath("$.result[0].fdSn").doesNotExist())
                .andExpect(jsonPath("$.result[0].source").doesNotExist())
                .andExpect(jsonPath("$.result[1].atcId").value("F2099010100000002"))
                .andExpect(jsonPath("$.result[5].atcId").value("F2099020100000003"));

        // 다시 실행해도 총 개수는 그대로
        mockMvc.perform(post("/search/save")).andExpect(status().isOk());
        mockMvc.perform(get("/search/total")).andExpect(jsonPath("$.result").value(6));
    }

    @DisplayName("일부 서비스만 실패하면 200 + 요약의 error")
    @Test
    void partialFailureReturns200() throws Exception {
        properties.setServiceKey(KEY);
        LOST112.respondWith(request -> {
            if (request.getUrl().encodedPath().equals(POLICE_PATH)) {
                return xml(500, "error");
            }
            return xml("portal-page" + request.getUrl().queryParameter("pageNo") + ".xml");
        });

        mockMvc.perform(post("/search/save"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.services[0].error").value("HTTP 500"))
                .andExpect(jsonPath("$.result.services[1].indexed").value(3));
    }

    @DisplayName("전부 실패해 하나도 못 넣으면 502, 메시지에 원인 코드가 있고 키는 없다")
    @Test
    void allFailedReturns502() throws Exception {
        properties.setServiceKey(KEY);
        LOST112.respondWith(request -> xml("gateway-30.xml"));

        mockMvc.perform(post("/search/save"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value(containsString("30 SERVICE_KEY_IS_NOT_REGISTERED_ERROR")))
                .andExpect(content().string(not(containsString("ab+c"))))
                .andExpect(content().string(not(containsString("ab%2Bc"))));

        mockMvc.perform(get("/search/total")).andExpect(jsonPath("$.result").value(0));
    }
}
