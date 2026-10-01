package com.findear.batch.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.findear.batch.common.job.BatchJobRunner;
import com.findear.batch.common.job.JobRunSummary;
import com.findear.batch.ours.domain.FindearMatchingLog;
import com.findear.batch.support.IntegrationTestBase;
import com.findear.batch.support.MatchMock;
import com.findear.batch.support.MatchResponses;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * findearJob (R-34): 대상 분실물 선정, 습득물 후보 조건과 acquiredBoardId(board_id), 분실물 하나의 실패 격리, 결과 없는 분실물.
 * match는 mock 서버다 (D-38).
 */
class FindearJobTest extends IntegrationTestBase {

    private static final LocalDate LOST_AT = LocalDate.now().minusDays(3);

    @Autowired
    BatchJobRunner jobRunner;
    @Autowired
    @Qualifier("findearJob")
    Job findearJob;

    private void insertLost(long lostBoardId, long boardId, String status, boolean deleted) {
        insertBoard(boardId, 1, true, "지갑", "검정", "검은색 지갑 " + lostBoardId, "검정 가죽 지갑", LocalDateTime.now().minusDays(3), status, deleted);
        insertLostBoard(lostBoardId, boardId, LOST_AT, 126.9707f, 37.5547f);
    }

    private void insertAcquired(long acquiredBoardId, long boardId, String category, LocalDateTime registeredAt, String status, boolean deleted) {
        insertBoard(boardId, 1, false, category, "검정", "습득물 " + boardId, "습득 설명", registeredAt, status, deleted);
        insertAcquiredBoard(acquiredBoardId, boardId, 126.97f, 37.55f);
    }

    private List<FindearMatchingLog> logs() {
        List<FindearMatchingLog> logs = new ArrayList<>();
        findearMatchingLogRepository.findAll().forEach(logs::add);
        return logs;
    }

    private FindearMatchingLog log(String id, long lostBoardId, long acquiredBoardId, float rate) {
        return FindearMatchingLog.builder().findearMatchingLogId(id).lostBoardId(lostBoardId).acquiredBoardId(acquiredBoardId)
                .similarityRate(rate).matchingAt(LocalDateTime.now().withNano(0)).build();
    }

    @DisplayName("대상은 진행 중·삭제 안 된 분실물만, 후보는 같은 카테고리·분실일 이후·진행 중·삭제 안 된 습득물만이고 acquiredBoardId는 board_id")
    @Test
    void targetsAndCandidates() {
        insertMember(1);
        insertLost(1, 10, "ONGOING", false);   // 대상
        insertLost(2, 20, "DONE", false);      // 찾음 -> 제외
        insertLost(3, 30, "ONGOING", true);    // 삭제 -> 제외
        LocalDateTime recent = LocalDateTime.now().minusDays(2);
        insertAcquired(7, 11, "지갑", recent, "ONGOING", false);                       // 후보 (PK 7, board_id 11)
        insertAcquired(8, 12, "지갑", recent, "ONGOING", true);                        // 삭제 -> 제외
        insertAcquired(9, 13, "지갑", recent, "DONE", false);                          // 반환 완료 -> 제외
        insertAcquired(10, 14, "전자기기", recent, "ONGOING", false);                  // 다른 카테고리 -> 제외
        insertAcquired(11, 15, "지갑", LocalDateTime.now().minusDays(5), "ONGOING", false); // 분실일 이전 등록 -> 제외
        insertAcquired(12, 16, "지갑", LOST_AT.atStartOfDay(), "ONGOING", false);      // 분실일 0시 정각 -> 후보
        // API(분실물 등록 직후 매칭)가 만든 board_id 기반 로그. 잡이 같은 쌍을 덮어쓰고 지우지 않아야 한다
        findearMatchingLogRepository.save(log("1-11", 1, 11, 0.1f));
        MATCH.respondWith(MatchResponses::respond);

        JobRunSummary summary = jobRunner.run(findearJob);

        assertThat(summary.status()).isEqualTo("COMPLETED");
        assertThat(summary.steps()).hasSize(1);
        JobRunSummary.StepSummary step = summary.steps().get(0);
        assertThat(step.stepName()).isEqualTo("findearMatchingStep");
        assertThat(step.status()).isEqualTo("COMPLETED");
        assertThat(step.processed()).isEqualTo(1);
        assertThat(step.succeeded()).isEqualTo(1);
        assertThat(step.failed()).isZero();

        List<RecordedRequest> requests = MATCH.requests();
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getUrl().encodedPath()).isEqualTo("/matching/findear");
        JsonNode body = MatchResponses.body(requests.get(0));
        assertThat(body.get("lostBoard").get("lostBoardId").asText()).isEqualTo("1");
        List<String> candidateIds = new ArrayList<>();
        body.get("acquiredBoardList").forEach(c -> candidateIds.add(c.get("acquiredBoardId").asText()));
        assertThat(candidateIds).containsExactlyInAnyOrder("11", "16"); // 습득물 PK(7, 12)가 아니라 board_id

        List<FindearMatchingLog> logs = logs();
        assertThat(logs).extracting(FindearMatchingLog::getFindearMatchingLogId).containsExactlyInAnyOrder("1-11", "1-16");
        assertThat(logs).extracting(FindearMatchingLog::getAcquiredBoardId).containsExactlyInAnyOrder(11L, 16L);
        assertThat(logs).allSatisfy(l -> {
            assertThat(l.getLostBoardId()).isEqualTo(1L);
            assertThat(l.getSimilarityRate()).isEqualTo(0.8f); // 0.1이던 1-11도 덮어씀
        });
    }

    @DisplayName("대상 분실물이 없으면 match를 부르지 않고 COMPLETED (처리 0)")
    @Test
    void noTargets() {
        insertMember(1);
        insertLost(2, 20, "DONE", false);
        insertLost(3, 30, "ONGOING", true);

        JobRunSummary summary = jobRunner.run(findearJob);

        assertThat(summary.status()).isEqualTo("COMPLETED");
        assertThat(summary.steps().get(0).processed()).isZero();
        assertThat(MATCH.requests()).isEmpty();
    }

    @DisplayName("분실물 하나에서 match가 500이어도 나머지는 처리되고 잡은 COMPLETED, 실패한 분실물의 기존 로그는 그대로")
    @Test
    void failureIsIsolated() {
        insertMember(1);
        insertLost(1, 10, "ONGOING", false);
        insertLost(2, 20, "ONGOING", false);
        insertLost(3, 30, "ONGOING", false);
        insertAcquired(7, 11, "지갑", LocalDateTime.now().minusDays(2), "ONGOING", false);
        findearMatchingLogRepository.save(log("2-99", 2, 99, 0.4f)); // 실패할 분실물의 기존 로그
        MATCH.respondWith(request -> MatchResponses.lostBoardId(request) == 2
                ? MatchMock.json(500, "{\"message\":\"서버 오류\"}")
                : MatchResponses.respond(request));

        JobRunSummary summary = jobRunner.run(findearJob);

        assertThat(summary.status()).isEqualTo("COMPLETED");
        JobRunSummary.StepSummary step = summary.steps().get(0);
        assertThat(step.status()).isEqualTo("COMPLETED");
        assertThat(step.processed()).isEqualTo(3);
        assertThat(step.succeeded()).isEqualTo(2);
        assertThat(step.failed()).isEqualTo(1);
        assertThat(MATCH.requests()).hasSize(3);
        assertThat(logs()).extracting(FindearMatchingLog::getFindearMatchingLogId).containsExactlyInAnyOrder("1-11", "3-11", "2-99");
    }

    @DisplayName("시도한 분실물이 전부 실패하면 스텝과 잡이 FAILED이고 기존 로그는 그대로")
    @Test
    void allFailedFailsStep() {
        insertMember(1);
        insertLost(1, 10, "ONGOING", false);
        insertLost(2, 20, "ONGOING", false);
        insertLost(3, 30, "ONGOING", false);
        findearMatchingLogRepository.save(log("1-99", 1, 99, 0.4f));
        MATCH.respondWith(request -> MatchMock.json(500, "{\"message\":\"서버 오류\"}"));

        JobRunSummary summary = jobRunner.run(findearJob);

        assertThat(summary.status()).isEqualTo("FAILED");
        JobRunSummary.StepSummary step = summary.steps().get(0);
        assertThat(step.status()).isEqualTo("FAILED");
        assertThat(step.processed()).isEqualTo(3);
        assertThat(step.succeeded()).isZero();
        assertThat(step.failed()).isEqualTo(3);
        assertThat(step.message()).contains("모두 실패");
        assertThat(logs()).extracting(FindearMatchingLog::getFindearMatchingLogId).containsExactly("1-99");
    }

    @DisplayName("result가 null인 분실물이 앞에 있어도 다음 분실물을 계속 처리한다 (옛 조기 return 버그), null이면 그 분실물의 로그만 지운다")
    @Test
    void nullResultDoesNotStopTheJob() {
        insertMember(1);
        insertLost(1, 10, "ONGOING", false);
        insertLost(2, 20, "ONGOING", false);
        insertAcquired(7, 11, "지갑", LocalDateTime.now().minusDays(2), "ONGOING", false);
        findearMatchingLogRepository.save(log("1-99", 1, 99, 0.4f));
        MATCH.respondWith(request -> MatchResponses.lostBoardId(request) == 1
                ? MatchMock.json(200, "{\"message\":\"없습니다\",\"result\":null}")
                : MatchResponses.respond(request));

        JobRunSummary summary = jobRunner.run(findearJob);

        assertThat(summary.status()).isEqualTo("COMPLETED");
        assertThat(summary.steps().get(0).succeeded()).isEqualTo(2);
        assertThat(MATCH.requests()).hasSize(2);
        assertThat(logs()).extracting(FindearMatchingLog::getFindearMatchingLogId).containsExactly("2-11");
    }
}
