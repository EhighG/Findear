package com.findear.batch.profile;

import com.findear.batch.ours.controller.LocalFindearDataController;
import com.findear.batch.ours.controller.LocalPoliceDataController;
import com.findear.batch.ours.domain.FindearMatchingLog;
import com.findear.batch.ours.domain.PoliceMatchingLog;
import com.findear.batch.police.controller.LocalPoliceAcquiredDataController;
import com.findear.batch.support.IntegrationTestBase;
import com.findear.batch.support.PoliceDocs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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
 * local 프로필 (D-26): 개발용 전체 조회·전체 삭제 엔드포인트가 열려 있고, 삭제한 test 엔드포인트와 유지 엔드포인트는 그대로다.
 * 프로필이 다른 컨텍스트라 이 클래스가 끝나면 컨텍스트(와 컨테이너)를 닫는다.
 */
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LocalProfileEndpointsTest extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ApplicationContext context;

    private void saveLogs() {
        findearMatchingLogRepository.save(FindearMatchingLog.builder().findearMatchingLogId("1-11").lostBoardId(1L)
                .acquiredBoardId(11L).similarityRate(0.75f).matchingAt(LocalDateTime.of(2026, 10, 1, 10, 0, 0)).build());
        policeMatchingLogRepository.save(PoliceMatchingLog.builder().policeMatchingLogId("1-F1").lostBoardId(1L).similarityRate(0.7f)
                .matchingAt(LocalDateTime.of(2026, 10, 1, 10, 0, 0)).atcId("F1").build());
        policeAcquiredDataRepository.save(PoliceDocs.doc("F1", "지갑", LocalDate.now()));
        policeAcquiredDataRepository.save(PoliceDocs.doc("F2", "지갑", LocalDate.now()));
    }

    @DisplayName("local에는 개발용 컨트롤러 빈 3개가 있다")
    @Test
    void localControllersPresent() {
        assertThat(context.getBeanNamesForType(LocalFindearDataController.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(LocalPoliceDataController.class)).hasSize(1);
        assertThat(context.getBeanNamesForType(LocalPoliceAcquiredDataController.class)).hasSize(1);
    }

    @DisplayName("전체 조회: GET /findear(매칭 로그), GET /search/all(Lost112 전체), GET /search/save?page&size(이름과 달리 조회, page는 0부터)는 200")
    @Test
    void listEndpointsAreOpen() throws Exception {
        saveLogs();

        mockMvc.perform(get("/findear"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result.length()").value(1))
                .andExpect(jsonPath("$.result[0].findearMatchingLogId").value("1-11"));
        mockMvc.perform(get("/search/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(2));
        mockMvc.perform(get("/search/save").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content.length()").value(1))
                .andExpect(jsonPath("$.result.totalElements").value(2));
        // 이 조회는 page가 0부터이고, 범위를 벗어나면 400
        mockMvc.perform(get("/search/save").param("page", "-1").param("size", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(get("/search/save").param("page", "0").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @DisplayName("전체 삭제: DELETE /findear, /police, /search는 200이고 데이터가 지워진다")
    @Test
    void deleteEndpointsAreOpen() throws Exception {
        saveLogs();

        mockMvc.perform(delete("/findear")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value(200));
        assertThat(findearMatchingLogRepository.count()).isZero();

        mockMvc.perform(delete("/police")).andExpect(status().isOk());
        assertThat(policeMatchingLogRepository.count()).isZero();

        assertThat(policeAcquiredDataRepository.count()).isEqualTo(2);
        mockMvc.perform(delete("/search")).andExpect(status().isOk());
        assertThat(policeAcquiredDataRepository.count()).isZero();
    }

    @DisplayName("삭제한 test 엔드포인트는 local에서도 404")
    @Test
    void testEndpointsAreGone() throws Exception {
        mockMvc.perform(get("/findear/test-api")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mockMvc.perform(get("/police/test-api")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        mockMvc.perform(get("/search/test")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @DisplayName("유지 엔드포인트는 local에서도 그대로: 총개수·목록·매칭 목록·수동 실행 API·Lost112 수집(키 없음 503)")
    @Test
    void keptEndpointsWork() throws Exception {
        saveLogs();

        mockMvc.perform(get("/search/total")).andExpect(status().isOk()).andExpect(jsonPath("$.result").value(2));
        mockMvc.perform(get("/search").param("page", "1").param("size", "10")).andExpect(status().isOk());
        mockMvc.perform(get("/findear/member/1")).andExpect(status().isOk()).andExpect(jsonPath("$.result.totalCount").value(0));
        mockMvc.perform(get("/police/member/1")).andExpect(status().isOk());
        mockMvc.perform(post("/findear/matching/batch")).andExpect(status().isOk()).andExpect(jsonPath("$.result.jobName").value("findearJob"));
        mockMvc.perform(post("/police/matching/batch")).andExpect(status().isOk()).andExpect(jsonPath("$.result.jobName").value("policeJob"));
        mockMvc.perform(post("/search/save")).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.status").value(503));
    }
}
