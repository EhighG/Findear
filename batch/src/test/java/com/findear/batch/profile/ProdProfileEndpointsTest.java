package com.findear.batch.profile;

import com.findear.batch.ours.controller.FindearDataController;
import com.findear.batch.ours.controller.LocalFindearDataController;
import com.findear.batch.ours.controller.LocalPoliceDataController;
import com.findear.batch.ours.controller.MatchingBatchController;
import com.findear.batch.ours.controller.PoliceDataController;
import com.findear.batch.ours.domain.FindearMatchingLog;
import com.findear.batch.ours.domain.PoliceMatchingLog;
import com.findear.batch.police.controller.LocalPoliceAcquiredDataController;
import com.findear.batch.police.controller.PoliceAcquiredDataController;
import com.findear.batch.support.IntegrationTestBase;
import com.findear.batch.support.MatchResponses;
import com.findear.batch.support.PoliceDocs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * prod 프로필 (D-26): 개발용 전체 조회·전체 삭제 엔드포인트는 없다(404, 같은 경로에 다른 메서드가 있으면 405).
 * 삭제한 test 엔드포인트도 404이고, main이 쓰는 엔드포인트와 내부 운영용 수동 실행 API는 그대로 동작한다.
 * 프로필이 다른 컨텍스트라 이 클래스가 끝나면 컨텍스트(와 컨테이너)를 닫는다.
 */
@ActiveProfiles("prod")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ProdProfileEndpointsTest extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ApplicationContext context;

    private void saveData() {
        findearMatchingLogRepository.save(FindearMatchingLog.builder().findearMatchingLogId("1-11").lostBoardId(1L)
                .acquiredBoardId(11L).similarityRate(0.75f).matchingAt(LocalDateTime.of(2026, 10, 1, 10, 0, 0)).build());
        policeMatchingLogRepository.save(PoliceMatchingLog.builder().policeMatchingLogId("1-F1").lostBoardId(1L).similarityRate(0.7f)
                .matchingAt(LocalDateTime.of(2026, 10, 1, 10, 0, 0)).atcId("F1").build());
        policeAcquiredDataRepository.save(PoliceDocs.doc("F1", "지갑", LocalDate.now()));
    }

    @DisplayName("prod에는 개발용 컨트롤러 빈이 없고, 일반 컨트롤러는 있다")
    @Test
    void localControllersAbsent() {
        assertThat(context.getBeanNamesForType(LocalFindearDataController.class)).isEmpty();
        assertThat(context.getBeanNamesForType(LocalPoliceDataController.class)).isEmpty();
        assertThat(context.getBeanNamesForType(LocalPoliceAcquiredDataController.class)).isEmpty();
        assertThat(context.getBeanNamesForType(FindearDataController.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(PoliceDataController.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(PoliceAcquiredDataController.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(MatchingBatchController.class)).hasSize(1);
    }

    @DisplayName("전체 조회는 404 (GET /search/save는 같은 경로에 POST가 있어 405), 응답은 공통 오류 형식")
    @Test
    void listEndpointsAreClosed() throws Exception {
        saveData();

        mockMvc.perform(get("/findear")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mockMvc.perform(get("/search/all")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mockMvc.perform(get("/search/save").param("page", "0").param("size", "10"))
                .andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.status").value(405));
    }

    @DisplayName("전체 삭제는 404 (DELETE /search는 같은 경로에 GET이 있어 405)이고 데이터는 그대로")
    @Test
    void deleteEndpointsAreClosed() throws Exception {
        saveData();

        mockMvc.perform(delete("/findear")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mockMvc.perform(delete("/police")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mockMvc.perform(delete("/search")).andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.status").value(405));

        assertThat(findearMatchingLogRepository.count()).isEqualTo(1);
        assertThat(policeMatchingLogRepository.count()).isEqualTo(1);
        assertThat(policeAcquiredDataRepository.count()).isEqualTo(1);
    }

    @DisplayName("삭제한 test 엔드포인트는 404")
    @Test
    void testEndpointsAreGone() throws Exception {
        mockMvc.perform(get("/findear/test-api")).andExpect(status().isNotFound());
        mockMvc.perform(get("/police/test-api")).andExpect(status().isNotFound());
        mockMvc.perform(get("/search/test")).andExpect(status().isNotFound());
    }

    @DisplayName("유지 엔드포인트는 prod에서도 동작: 목록·총개수·스크랩·매칭 목록·분실물 매칭·수동 실행 API·Lost112 수집(키 없음 503)")
    @Test
    void keptEndpointsWork() throws Exception {
        saveData();
        insertMember(1);
        insertBoard(10, 1, true, "지갑", "검정", "지갑", "d", LocalDateTime.now().minusDays(3));
        insertLostBoard(1, 10, LocalDate.now().minusDays(3), 1f, 1f);
        MATCH.respondWith(MatchResponses::respond);

        mockMvc.perform(get("/search/total")).andExpect(status().isOk()).andExpect(jsonPath("$.result").value(1));
        mockMvc.perform(get("/search").param("page", "1").param("size", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.length()").value(1));
        mockMvc.perform(post("/police/scrap").contentType(MediaType.APPLICATION_JSON).content("{\"atcIdList\":[\"F1\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result.length()").value(1));
        mockMvc.perform(get("/findear/board/1")).andExpect(status().isOk()).andExpect(jsonPath("$.result.totalCount").value(1));
        mockMvc.perform(get("/findear/member/1")).andExpect(status().isOk());
        mockMvc.perform(get("/police/board/1")).andExpect(status().isOk());
        mockMvc.perform(get("/police/member/1")).andExpect(status().isOk());
        mockMvc.perform(get("/findear/board/999")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mockMvc.perform(post("/findear/matching").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lostBoardId\":\"1\",\"productName\":\"지갑\",\"color\":\"검정\",\"categoryName\":\"지갑\",\"description\":\"d\","
                                + "\"lostAt\":\"" + LocalDate.now().minusDays(3) + "\",\"xpos\":\"1.0\",\"ypos\":\"1.0\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(200));
        mockMvc.perform(post("/findear/matching/batch")).andExpect(status().isOk()).andExpect(jsonPath("$.result.jobName").value("findearJob"));
        mockMvc.perform(post("/police/matching/batch")).andExpect(status().isOk()).andExpect(jsonPath("$.result.jobName").value("policeJob"));
        mockMvc.perform(post("/search/save")).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.status").value(503));
    }
}
