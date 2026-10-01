package com.findear.batch.job;

import com.findear.batch.common.job.BatchJobRunner;
import com.findear.batch.ours.job.scheduler.FindearJobScheduler;
import com.findear.batch.support.IntegrationTestBase;
import com.findear.batch.support.MatchMock;
import com.findear.batch.support.MatchResponses;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 수동 실행 API (07 §3): POST /findear/matching/batch -> findearJob, POST /police/matching/batch -> policeJob.
 * 잡을 실행하므로 Spring Batch 메타 테이블(BATCH_JOB_EXECUTION)에 기록이 남는다. 같은 잡이 이미 돌고 있으면 409.
 */
class MatchingBatchApiTest extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    BatchJobRunner jobRunner;
    @Autowired
    @Qualifier("findearJob")
    Job findearJob;

    private void insertTargetLost() {
        insertMember(1);
        insertBoard(10, 1, true, "지갑", "검정", "검은색 지갑", "검정 가죽 지갑", LocalDateTime.now().minusDays(3));
        insertLostBoard(1, 10, LocalDate.now().minusDays(3), 126.9707f, 37.5547f);
        insertBoard(11, 1, false, "지갑", "검정", "검은색 반지갑", "검정 반지갑", LocalDateTime.now().minusDays(2));
        insertAcquiredBoard(7, 11, 126.97f, 37.55f);
    }

    private int executions(String jobName, String status) {
        return jdbc.queryForObject("select count(*) from BATCH_JOB_EXECUTION e join BATCH_JOB_INSTANCE i on e.JOB_INSTANCE_ID = i.JOB_INSTANCE_ID "
                + "where i.JOB_NAME = ? and e.STATUS = ?", Integer.class, jobName, status);
    }

    @DisplayName("POST /findear/matching/batch: findearJob 실행 -> 200 + 실행 요약, 메타 테이블에 COMPLETED 기록, board_id 로그")
    @Test
    void runsFindearJob() throws Exception {
        insertTargetLost();
        MATCH.respondWith(MatchResponses::respond);
        int before = executions("findearJob", "COMPLETED");

        mockMvc.perform(post("/findear/matching/batch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result.jobExecutionId").isNumber())
                .andExpect(jsonPath("$.result.jobName").value("findearJob"))
                .andExpect(jsonPath("$.result.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result.durationMillis").isNumber())
                .andExpect(jsonPath("$.result.steps.length()").value(1))
                .andExpect(jsonPath("$.result.steps[0].stepName").value("findearMatchingStep"))
                .andExpect(jsonPath("$.result.steps[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.result.steps[0].processed").value(1))
                .andExpect(jsonPath("$.result.steps[0].succeeded").value(1))
                .andExpect(jsonPath("$.result.steps[0].failed").value(0));

        assertThat(executions("findearJob", "COMPLETED")).isEqualTo(before + 1);
        assertThat(findearMatchingLogRepository.findById("1-11")).isPresent();
    }

    @DisplayName("POST /police/matching/batch: policeJob 실행(수집은 꺼짐) -> 200 + 수집·매칭 스텝 요약, 메타 테이블에 기록")
    @Test
    void runsPoliceJob() throws Exception {
        insertTargetLost();
        policeAcquiredDataRepository.save(com.findear.batch.support.PoliceDocs.doc("F7001", "지갑", LocalDate.now()));
        MATCH.respondWith(MatchResponses::respond);
        int before = executions("policeJob", "COMPLETED");

        mockMvc.perform(post("/police/matching/batch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.jobName").value("policeJob"))
                .andExpect(jsonPath("$.result.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result.steps.length()").value(2))
                .andExpect(jsonPath("$.result.steps[0].stepName").value("policeSaveStep"))
                .andExpect(jsonPath("$.result.steps[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.result.steps[1].stepName").value("policeMatchingStep"))
                .andExpect(jsonPath("$.result.steps[1].processed").value(1))
                .andExpect(jsonPath("$.result.steps[1].succeeded").value(1));

        assertThat(LOST112.requests()).isEmpty();
        assertThat(executions("policeJob", "COMPLETED")).isEqualTo(before + 1);
        assertThat(policeMatchingLogRepository.findById("1-F7001")).isPresent();
    }

    @DisplayName("match가 모두 실패하면 잡은 FAILED로 기록되고 응답은 200 + status FAILED, 스텝 실패 원인 요약")
    @Test
    void failedJobIsReportedInSummary() throws Exception {
        insertTargetLost();
        MATCH.respondWith(request -> MatchMock.json(500, "{\"message\":\"서버 오류\"}"));
        int before = executions("findearJob", "FAILED");

        mockMvc.perform(post("/findear/matching/batch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("FAILED"))
                .andExpect(jsonPath("$.result.steps[0].status").value("FAILED"))
                .andExpect(jsonPath("$.result.steps[0].failed").value(1))
                .andExpect(jsonPath("$.result.steps[0].message").value(containsString("모두 실패")));

        assertThat(executions("findearJob", "FAILED")).isEqualTo(before + 1);
    }

    @DisplayName("같은 잡이 실행 중이면 API는 409, 스케줄 트리거는 건너뛰고, 끝나면 다시 실행된다")
    @Test
    void overlappingRunIsRejected() throws Exception {
        insertTargetLost();
        CountDownLatch release = new CountDownLatch(1);
        MATCH.respondWith(request -> {
            try {
                release.await(60, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return MatchResponses.respond(request);
        });
        int before = executions("findearJob", "COMPLETED");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // 첫 실행: match 응답이 풀릴 때까지 잡이 돌고 있다
            Future<MvcResult> first = executor.submit(() -> mockMvc.perform(post("/findear/matching/batch")).andReturn());
            await().atMost(30, TimeUnit.SECONDS).until(() -> MATCH.requests().size() == 1);

            mockMvc.perform(post("/findear/matching/batch"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value(containsString("이미 실행 중")));

            // 스케줄 트리거도 건너뛴다 (예외 없이 바로 돌아오고 새 실행이 생기지 않는다)
            new FindearJobScheduler(jobRunner, findearJob).jobScheduled();
            assertThat(MATCH.requests()).hasSize(1);

            release.countDown();
            MvcResult result = first.get(60, TimeUnit.SECONDS);
            assertThat(result.getResponse().getStatus()).isEqualTo(200);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }

        assertThat(executions("findearJob", "COMPLETED")).isEqualTo(before + 1);

        // 끝난 뒤에는 다시 실행된다
        mockMvc.perform(post("/findear/matching/batch")).andExpect(status().isOk());
        assertThat(executions("findearJob", "COMPLETED")).isEqualTo(before + 2);
    }
}
