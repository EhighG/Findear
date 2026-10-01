package com.findear.batch.job;

import com.findear.batch.support.IntegrationTestBase;
import com.findear.batch.support.MatchResponses;
import com.findear.batch.support.PoliceDocs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * 스케줄 (R-34): 짧은 cron(매초)을 설정하면 두 잡이 실제로 실행되어 매칭 로그가 쌓이고 잡 실행 기록이 남는다.
 * 스케줄이 켜진 별도 컨텍스트(와 컨테이너)를 쓰므로 끝나면 컨텍스트를 닫는다 (스케줄이 다른 테스트의 mock 요청을 건드리지 않게).
 * 겹침(실행 중 중복 트리거 건너뜀)은 MatchingBatchApiTest와 BatchJobRunnerTest에서 확인한다.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ScheduledJobsTest extends IntegrationTestBase {

    @DynamicPropertySource
    static void scheduling(DynamicPropertyRegistry registry) {
        registry.add("batch.scheduling.enabled", () -> "true");
        registry.add("batch.jobs.findear.cron", () -> "* * * * * *");
        registry.add("batch.jobs.police.cron", () -> "* * * * * *");
    }

    private int completedExecutions(String jobName) {
        return jdbc.queryForObject("select count(*) from BATCH_JOB_EXECUTION e join BATCH_JOB_INSTANCE i on e.JOB_INSTANCE_ID = i.JOB_INSTANCE_ID "
                + "where i.JOB_NAME = ? and e.STATUS = 'COMPLETED'", Integer.class, jobName);
    }

    @DisplayName("짧은 cron이면 findearJob·policeJob이 실제로 돌아 두 종류의 매칭 로그가 쌓이고, 두 잡 모두 COMPLETED 기록이 남는다")
    @Test
    void jobsRunOnSchedule() {
        MATCH.respondWith(MatchResponses::respond);
        insertMember(1);
        insertBoard(10, 1, true, "지갑", "검정", "검은색 지갑", "검정 가죽 지갑", LocalDateTime.now().minusDays(3));
        insertLostBoard(1, 10, LocalDate.now().minusDays(3), 126.9707f, 37.5547f);
        insertBoard(11, 1, false, "지갑", "검정", "검은색 반지갑", "검정 반지갑", LocalDateTime.now().minusDays(2));
        insertAcquiredBoard(7, 11, 126.97f, 37.55f);
        policeAcquiredDataRepository.save(PoliceDocs.doc("F8001", "지갑", LocalDate.now()));
        int findearBefore = completedExecutions("findearJob");
        int policeBefore = completedExecutions("policeJob");

        await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(findearMatchingLogRepository.findById("1-11")).isPresent();
            assertThat(policeMatchingLogRepository.findById("1-F8001")).isPresent();
            assertThat(completedExecutions("findearJob")).isGreaterThan(findearBefore);
            assertThat(completedExecutions("policeJob")).isGreaterThan(policeBefore);
        });
        assertThat(LOST112.requests()).isEmpty(); // 수집은 꺼짐
    }
}
