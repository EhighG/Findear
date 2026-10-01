package com.findear.batch;

import com.findear.batch.ours.domain.FindearMatchingLog;
import com.findear.batch.ours.domain.PoliceMatchingLog;
import com.findear.batch.police.domain.PoliceAcquiredData;
import com.findear.batch.support.IntegrationTestBase;
import com.findear.batch.support.MatchMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 컨트롤러 응답 JSON 모양(07 §2): {status, message, result}. main이 읽는 필드 이름이 그대로인지 확인한다.
 */
class ApiResponseTest extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    private PoliceAcquiredData wallet() {
        return PoliceAcquiredData.builder().id("F1").atcId("F1").depPlace("종로경찰서").fdFilePathImg("https://img/1").fdPrdtNm("지갑")
                .fdSbjt("검정 지갑").clrNm("검정").fdYmd(LocalDate.now()).prdtClNm("지갑 > 반지갑").mainPrdtClNm("지갑")
                .subPrdtClNm("반지갑").fdSn("1").source("POLICE").build();
    }

    @DisplayName("GET /search/total, GET /search: result가 숫자·목록")
    @Test
    void searchEndpoints() throws Exception {
        mockMvc.perform(get("/search/total"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result").value(0));
        mockMvc.perform(get("/search").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").isEmpty());

        policeAcquiredDataRepository.save(wallet());

        mockMvc.perform(get("/search/total")).andExpect(jsonPath("$.result").value(1));
        mockMvc.perform(get("/search").param("page", "1").param("size", "10").param("category", "지갑"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(1))
                .andExpect(jsonPath("$.result[0].id").value("F1"))
                .andExpect(jsonPath("$.result[0].atcId").value("F1"))
                .andExpect(jsonPath("$.result[0].depPlace").value("종로경찰서"))
                .andExpect(jsonPath("$.result[0].fdFilePathImg").value("https://img/1"))
                .andExpect(jsonPath("$.result[0].fdPrdtNm").value("지갑"))
                .andExpect(jsonPath("$.result[0].fdSbjt").value("검정 지갑"))
                .andExpect(jsonPath("$.result[0].clrNm").value("검정"))
                .andExpect(jsonPath("$.result[0].fdYmd").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.result[0].prdtClNm").value("지갑 > 반지갑"))
                .andExpect(jsonPath("$.result[0].mainPrdtClNm").value("지갑"))
                .andExpect(jsonPath("$.result[0].subPrdtClNm").value("반지갑"));
    }

    @DisplayName("POST /police/scrap: atcIdList → 목록과 같은 모양의 배열")
    @Test
    void scrapEndpoint() throws Exception {
        policeAcquiredDataRepository.save(wallet());

        mockMvc.perform(post("/police/scrap").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"atcIdList\":[\"F1\",\"없는ID\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(1))
                .andExpect(jsonPath("$.result[0].atcId").value("F1"))
                .andExpect(jsonPath("$.result[0].id").value("F1"));
    }

    @DisplayName("GET /findear/board/{id}, /findear/member/{id}: {matchingList, totalCount}, 항목은 matchedAt")
    @Test
    void findearListEndpoints() throws Exception {
        insertMember(1);
        insertBoard(10, 1, true, "지갑", "검정", "지갑", "d", LocalDateTime.now());
        insertLostBoard(1, 10, LocalDate.now(), 1f, 1f);
        findearMatchingLogRepository.save(FindearMatchingLog.builder().findearMatchingLogId(1L).lostBoardId(1L)
                .acquiredBoardId(11L).similarityRate(0.75f).matchingAt("2026-10-01T10:00:00").build());

        mockMvc.perform(get("/findear/board/1").param("page", "1").param("size", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalCount").value(1))
                .andExpect(jsonPath("$.result.matchingList[0].findearMatchingLogId").value(1))
                .andExpect(jsonPath("$.result.matchingList[0].lostBoardId").value(1))
                .andExpect(jsonPath("$.result.matchingList[0].acquiredBoardId").value(11))
                .andExpect(jsonPath("$.result.matchingList[0].similarityRate").value(0.75))
                .andExpect(jsonPath("$.result.matchingList[0].matchedAt").value("2026-10-01T10:00:00"));
        mockMvc.perform(get("/findear/member/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalCount").value(1))
                .andExpect(jsonPath("$.result.matchingList[0].lostBoardId").value(1));
    }

    @DisplayName("GET /police/board/{id}, /police/member/{id}: 항목은 policeMatchingLogId와 습득물 필드 사본(전부 문자열)")
    @Test
    void policeListEndpoints() throws Exception {
        insertMember(1);
        insertBoard(10, 1, true, "지갑", "검정", "지갑", "d", LocalDateTime.now());
        insertLostBoard(1, 10, LocalDate.now(), 1f, 1f);
        policeMatchingLogRepository.save(PoliceMatchingLog.builder().policeMatchingLogId(1L).lostBoardId(1L).similarityRate(0.7f)
                .matchingAt("2026-10-01T10:00:00").acquiredBoardId("5001").atcId("F1").depPlace("종로경찰서")
                .fdFilePathImg("https://img/1").fdPrdtNm("지갑").fdSbjt("검정 지갑").clrNm("검정").fdYmd("2026-09-30")
                .mainPrdtClNm("지갑").build());

        mockMvc.perform(get("/police/board/1").param("page", "1").param("size", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalCount").value(1))
                .andExpect(jsonPath("$.result.matchingList[0].policeMatchingLogId").value("1"))
                .andExpect(jsonPath("$.result.matchingList[0].lostBoardId").value("1"))
                .andExpect(jsonPath("$.result.matchingList[0].similarityRate").value("0.7"))
                .andExpect(jsonPath("$.result.matchingList[0].matchedAt").value("2026-10-01T10:00:00"))
                .andExpect(jsonPath("$.result.matchingList[0].acquiredBoardId").value("5001"))
                .andExpect(jsonPath("$.result.matchingList[0].atcId").value("F1"))
                .andExpect(jsonPath("$.result.matchingList[0].mainPrdtClNm").value("지갑"));
        mockMvc.perform(get("/police/member/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalCount").value(1));
    }

    @DisplayName("POST /findear/matching: main이 보내는 xpos/ypos 키의 본문을 받아 findearDatas·policeDatas를 돌려준다")
    @Test
    void matchingEndpoint() throws Exception {
        insertMember(1);
        insertBoard(10, 1, true, "지갑", "검정", "지갑", "d", LocalDateTime.now().minusDays(3));
        insertLostBoard(1, 10, LocalDate.now().minusDays(3), 1f, 1f);
        insertBoard(11, 1, false, "지갑", "검정", "반지갑", "d", LocalDateTime.now().minusDays(1));
        insertAcquiredBoard(7, 11, 1f, 1f);
        MATCH.respondWith(request -> request.getUrl().encodedPath().equals("/matching/findear")
                ? MatchMock.json(200,
                "{\"message\":\"ok\",\"result\":[{\"lostBoardId\":1,\"acquiredBoardId\":11,\"similarityRate\":0.9}]}")
                : MatchMock.json(200, "{\"message\":\"없음\",\"result\":null}"));

        mockMvc.perform(post("/findear/matching").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lostBoardId\":\"1\",\"productName\":\"지갑\",\"color\":\"검정\",\"categoryName\":\"지갑\","
                                + "\"description\":\"d\",\"lostAt\":\"" + LocalDate.now().minusDays(3) + "\",\"xpos\":\"1.0\",\"ypos\":\"1.0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result.findearDatas[0].lostBoardId").value(1))
                .andExpect(jsonPath("$.result.findearDatas[0].acquiredBoardId").value(11))
                .andExpect(jsonPath("$.result.findearDatas[0].similarityRate").value(0.9))
                .andExpect(jsonPath("$.result.policeDatas").isEmpty());
    }
}
