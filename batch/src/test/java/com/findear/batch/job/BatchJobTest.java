package com.findear.batch.job;

import com.findear.batch.ours.domain.FindearMatchingLog;
import com.findear.batch.support.IntegrationTestBase;
import com.findear.batch.support.MatchMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Batch 5 구성 확인: 잡·스텝 이름, 메타 테이블(Flyway V1), 실행마다 다른 date 파라미터로 다시 실행.
 * policeSaveStep(Lost112 수집)은 외부 API를 부르므로 실행하지 않는다 (policeJob은 policeMatchingStep만 실행한다).
 */
class BatchJobTest extends IntegrationTestBase {

    @Autowired
    JobLauncher jobLauncher;
    @Autowired
    @Qualifier("findearJob")
    Job findearJob;
    @Autowired
    @Qualifier("policeJob")
    Job policeJob;

    @DisplayName("잡 이름과 스텝 구성: findearJob(findearMatchingStep), policeJob(policeMatchingStep)")
    @Test
    void jobNames() {
        assertThat(findearJob.getName()).isEqualTo("findearJob");
        assertThat(policeJob.getName()).isEqualTo("policeJob");
    }

    @DisplayName("findearJob: 진행 중인 분실물마다 match를 호출하고 매칭 로그를 저장한다. date 파라미터만 바꾸면 다시 실행된다")
    @Test
    void findearJobRuns() throws Exception {
        insertMember(1);
        insertBoard(10, 1, true, "지갑", "검정", "검은색 가죽 지갑", "검정 가죽 지갑", LocalDateTime.now().minusDays(3));
        insertLostBoard(1, 10, LocalDate.now().minusDays(3), 126.9707f, 37.5547f);
        insertBoard(11, 1, false, "지갑", "검정", "검은색 반지갑", "검정 반지갑", LocalDateTime.now().minusDays(2));
        insertAcquiredBoard(7, 11, 126.97f, 37.55f);
        MATCH.respondWith(request -> MatchMock.json(200,
                "{\"message\":\"ok\",\"result\":[{\"lostBoardId\":1,\"acquiredBoardId\":7,\"similarityRate\":0.8}]}"));

        JobExecution first = jobLauncher.run(findearJob, new JobParametersBuilder().addString("date", "2026-10-01 10:00:00:000").toJobParameters());
        JobExecution second = jobLauncher.run(findearJob, new JobParametersBuilder().addString("date", "2026-10-01 10:00:00:001").toJobParameters());

        assertThat(first.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(second.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(first.getStepExecutions()).extracting(s -> s.getStepName()).containsExactly("findearMatchingStep");

        // 잡 로직은 지금 그대로다: 요청의 acquiredBoardId는 습득물 테이블 PK(7)이다 (게시글 board_id로 바꾸는 것은 R-34)
        assertThat(MATCH.requests()).hasSize(2);
        assertThat(MATCH.requests().get(0).getUrl().encodedPath()).isEqualTo("/matching/findear");
        assertThat(MATCH.requests().get(0).getBody().utf8()).contains("\"acquiredBoardId\":\"7\"");

        List<FindearMatchingLog> logs = new ArrayList<>();
        findearMatchingLogRepository.findAll().forEach(logs::add);
        assertThat(logs).hasSize(2);
        assertThat(logs).allSatisfy(l -> {
            assertThat(l.getLostBoardId()).isEqualTo(1L);
            assertThat(l.getAcquiredBoardId()).isEqualTo(7L);
        });
    }

    @DisplayName("policeJob: policeMatchingStep만 실행하고 외부 호출은 없다")
    @Test
    void policeJobRuns() throws Exception {
        JobExecution execution = jobLauncher.run(policeJob, new JobParametersBuilder().addString("date", "2026-10-01 10:00:00:000").toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions()).extracting(s -> s.getStepName()).containsExactly("policeMatchingStep");
        assertThat(MATCH.requests()).isEmpty();
    }
}
