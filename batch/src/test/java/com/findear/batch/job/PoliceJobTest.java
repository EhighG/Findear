package com.findear.batch.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.findear.batch.common.job.BatchJobRunner;
import com.findear.batch.common.job.JobRunSummary;
import com.findear.batch.ours.domain.PoliceMatchingLog;
import com.findear.batch.police.client.Lost112Properties;
import com.findear.batch.support.IntegrationTestBase;
import com.findear.batch.support.MatchMock;
import com.findear.batch.support.MatchResponses;
import com.findear.batch.support.PoliceDocs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.findear.batch.support.Lost112Fixtures.xml;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * policeJob (R-34): policeSaveStep(Lost112 수집, collect-enabled일 때만) -> policeMatchingStep(Lost112 매칭).
 * match와 Lost112는 모두 mock 서버다 (D-38, apis.data.go.kr로는 요청이 나가지 않는다).
 */
class PoliceJobTest extends IntegrationTestBase {

    private static final String POLICE_PATH = "/LosfundInfoInqireService/getLosfundInfoAccToClAreaPd";
    private static final String KEY = "ab+c/d==";
    private static final LocalDate LOST_AT = LocalDate.of(2026, 9, 1);

    @Autowired
    BatchJobRunner jobRunner;
    @Autowired
    @Qualifier("policeJob")
    Job policeJob;
    @Autowired
    Lost112Properties properties;

    private boolean originalCollectEnabled;
    private String originalKey;
    private int originalPageSize;

    @BeforeEach
    void saveProperties() {
        originalCollectEnabled = properties.isCollectEnabled();
        originalKey = properties.getServiceKey();
        originalPageSize = properties.getPageSize();
    }

    @AfterEach
    void restoreProperties() {
        properties.setCollectEnabled(originalCollectEnabled);
        properties.setServiceKey(originalKey);
        properties.setPageSize(originalPageSize);
    }

    private void insertLost(long lostBoardId, long boardId, String status, boolean deleted) {
        insertBoard(boardId, 1, true, "지갑", "검정", "검은색 지갑 " + lostBoardId, "검정 가죽 지갑", LocalDateTime.now().minusDays(3), status, deleted);
        insertLostBoard(lostBoardId, boardId, LOST_AT, 126.9707f, 37.5547f);
    }

    private void saveLost112Docs() {
        policeAcquiredDataRepository.saveAll(List.of(
                PoliceDocs.doc("F6001", "지갑", LOST_AT.plusDays(1)),     // 후보
                PoliceDocs.doc("F6002", "전자기기", LOST_AT.plusDays(1)), // 다른 카테고리
                PoliceDocs.doc("F6003", "지갑", LOST_AT.minusDays(1))));  // 분실일 이전
    }

    private List<PoliceMatchingLog> logs() {
        List<PoliceMatchingLog> logs = new ArrayList<>();
        policeMatchingLogRepository.findAll().forEach(logs::add);
        return logs;
    }

    private void respondWithPages() {
        LOST112.respondWith(request -> {
            String prefix = request.getUrl().encodedPath().equals(POLICE_PATH) ? "police-page" : "portal-page";
            String pageNo = request.getUrl().queryParameter("pageNo");
            return "1".equals(pageNo) || "2".equals(pageNo) ? xml(prefix + pageNo + ".xml") : xml("no-data.xml");
        });
    }

    @DisplayName("수집이 꺼져 있으면(기본) 수집 요청 0건, 수집 스텝은 COMPLETED이고 매칭 스텝은 ES의 Lost112 문서로 매칭해 로그를 저장한다")
    @Test
    void collectDisabledStillMatches() {
        assertThat(properties.isCollectEnabled()).isFalse(); // 기본값
        insertMember(1);
        insertLost(1, 10, "ONGOING", false);   // 대상
        insertLost(2, 20, "DONE", false);      // 제외
        insertLost(3, 30, "ONGOING", true);    // 제외
        saveLost112Docs();
        MATCH.respondWith(MatchResponses::respond);

        JobRunSummary summary = jobRunner.run(policeJob);

        assertThat(summary.status()).isEqualTo("COMPLETED");
        assertThat(summary.steps()).extracting(JobRunSummary.StepSummary::stepName).containsExactly("policeSaveStep", "policeMatchingStep");
        assertThat(summary.steps()).extracting(JobRunSummary.StepSummary::status).containsExactly("COMPLETED", "COMPLETED");
        assertThat(LOST112.requests()).isEmpty();

        assertThat(MATCH.requests()).hasSize(1);
        assertThat(MATCH.requests().get(0).getUrl().encodedPath()).isEqualTo("/matching/lost");
        JsonNode body = MatchResponses.body(MATCH.requests().get(0));
        assertThat(body.get("lostBoard").get("lostBoardId").asText()).isEqualTo("1");
        List<String> atcIds = new ArrayList<>();
        body.get("acquiredBoardList").forEach(c -> atcIds.add(c.get("atcId").asText()));
        assertThat(atcIds).containsExactly("F6001");

        JobRunSummary.StepSummary matching = summary.steps().get(1);
        assertThat(matching.processed()).isEqualTo(1);
        assertThat(matching.succeeded()).isEqualTo(1);
        assertThat(matching.failed()).isZero();
        assertThat(logs()).extracting(PoliceMatchingLog::getPoliceMatchingLogId).containsExactly("1-F6001");
        assertThat(logs().get(0).getLostBoardId()).isEqualTo(1L);
        assertThat(logs().get(0).getAtcId()).isEqualTo("F6001");
        assertThat(logs().get(0).getSimilarityRate()).isEqualTo(0.7f);
    }

    @DisplayName("수집을 켜고 Lost112 mock이 응답하면 수집한 문서로 매칭한다")
    @Test
    void collectEnabledCollectsThenMatches() {
        properties.setCollectEnabled(true);
        properties.setServiceKey(KEY);
        properties.setPageSize(2);
        respondWithPages();
        insertMember(1);
        insertLost(1, 10, "ONGOING", false);
        MATCH.respondWith(MatchResponses::respond);

        JobRunSummary summary = jobRunner.run(policeJob);

        assertThat(summary.status()).isEqualTo("COMPLETED");
        assertThat(summary.steps()).extracting(JobRunSummary.StepSummary::status).containsExactly("COMPLETED", "COMPLETED");
        assertThat(summary.steps().get(0).succeeded()).isEqualTo(6); // 인덱스에 넣은 문서 수
        assertThat(LOST112.requests()).hasSize(4);
        assertThat(policeAcquiredDataRepository.count()).isEqualTo(6);

        assertThat(MATCH.requests()).hasSize(1);
        JsonNode body = MatchResponses.body(MATCH.requests().get(0));
        List<String> atcIds = new ArrayList<>();
        body.get("acquiredBoardList").forEach(c -> atcIds.add(c.get("atcId").asText()));
        assertThat(atcIds).contains("F2099010100000001", "F2099010100000002");
        assertThat(logs()).extracting(PoliceMatchingLog::getAtcId).contains("F2099010100000001", "F2099010100000002");
        assertThat(logs()).extracting(PoliceMatchingLog::getLostBoardId).containsOnly(1L);
    }

    @DisplayName("수집을 켰는데 키가 없으면 수집 스텝만 실패(FAILED 종료 코드, 요청 0건)이고 매칭 스텝은 실행된다. 잡은 매칭 결과를 따라 COMPLETED")
    @Test
    void collectEnabledWithoutKeyFailsOnlyTheSaveStep() {
        properties.setCollectEnabled(true);
        properties.setServiceKey("");
        insertMember(1);
        insertLost(1, 10, "ONGOING", false);
        saveLost112Docs();
        MATCH.respondWith(MatchResponses::respond);

        JobRunSummary summary = jobRunner.run(policeJob);

        assertThat(summary.steps()).extracting(JobRunSummary.StepSummary::stepName).containsExactly("policeSaveStep", "policeMatchingStep");
        // 수집 스텝이 실패했는데 흐름이 매칭 스텝으로 이어지면 Spring Batch가 실패한 스텝의 상태를 ABANDONED로 바꾼다 (종료 코드는 FAILED 그대로)
        assertThat(summary.steps()).extracting(JobRunSummary.StepSummary::status).containsExactly("ABANDONED", "COMPLETED");
        assertThat(summary.steps()).extracting(JobRunSummary.StepSummary::exitCode).containsExactly("FAILED", "COMPLETED");
        assertThat(summary.steps().get(0).message()).contains("Lost112NotConfiguredException").contains("API 키가 설정되지 않았습니다");
        assertThat(summary.status()).isEqualTo("COMPLETED"); // 잡 상태는 마지막 스텝을 따른다. 수집 실패는 스텝 상태와 로그에 남는다
        assertThat(LOST112.requests()).isEmpty();
        assertThat(MATCH.requests()).hasSize(1);
        assertThat(logs()).extracting(PoliceMatchingLog::getPoliceMatchingLogId).containsExactly("1-F6001");
    }

    @DisplayName("수집을 켰는데 Lost112 호출이 모두 실패하면 수집 스텝 실패, 매칭 스텝은 이미 쌓인 문서로 실행")
    @Test
    void collectEnabledAllCallsFailed() {
        properties.setCollectEnabled(true);
        properties.setServiceKey(KEY);
        LOST112.respondWith(request -> xml(500, "error"));
        insertMember(1);
        insertLost(1, 10, "ONGOING", false);
        saveLost112Docs();
        MATCH.respondWith(MatchResponses::respond);

        JobRunSummary summary = jobRunner.run(policeJob);

        assertThat(summary.steps()).extracting(JobRunSummary.StepSummary::status).containsExactly("ABANDONED", "COMPLETED");
        assertThat(summary.steps()).extracting(JobRunSummary.StepSummary::exitCode).containsExactly("FAILED", "COMPLETED");
        assertThat(summary.steps().get(0).message()).contains("Lost112UnavailableException").doesNotContain("ab+c").doesNotContain("ab%2Bc");
        assertThat(LOST112.requests()).isNotEmpty();
        assertThat(logs()).extracting(PoliceMatchingLog::getPoliceMatchingLogId).containsExactly("1-F6001");
    }

    @DisplayName("분실물 하나의 match 실패는 건너뛰고 계속, 전부 실패하면 매칭 스텝과 잡이 FAILED")
    @Test
    void matchingFailureHandling() {
        insertMember(1);
        insertLost(1, 10, "ONGOING", false);
        insertLost(2, 20, "ONGOING", false);
        saveLost112Docs();

        MATCH.respondWith(request -> MatchResponses.lostBoardId(request) == 2
                ? MatchMock.json(500, "{\"message\":\"서버 오류\"}")
                : MatchResponses.respond(request));
        JobRunSummary partial = jobRunner.run(policeJob);

        assertThat(partial.status()).isEqualTo("COMPLETED");
        JobRunSummary.StepSummary matching = partial.steps().get(1);
        assertThat(matching.processed()).isEqualTo(2);
        assertThat(matching.succeeded()).isEqualTo(1);
        assertThat(matching.failed()).isEqualTo(1);
        assertThat(logs()).extracting(PoliceMatchingLog::getPoliceMatchingLogId).containsExactly("1-F6001");

        MATCH.respondWith(request -> MatchMock.json(500, "{\"message\":\"서버 오류\"}"));
        JobRunSummary allFailed = jobRunner.run(policeJob);

        assertThat(allFailed.status()).isEqualTo("FAILED");
        assertThat(allFailed.steps()).extracting(JobRunSummary.StepSummary::status).containsExactly("COMPLETED", "FAILED");
        assertThat(allFailed.steps().get(1).failed()).isEqualTo(2);
        assertThat(logs()).extracting(PoliceMatchingLog::getPoliceMatchingLogId).containsExactly("1-F6001"); // 기존 로그 유지
    }

    @DisplayName("Lost112 후보가 없는 분실물은 그 분실물의 기존 로그를 지운다 (result null)")
    @Test
    void noCandidatesClearsLogs() {
        insertMember(1);
        insertLost(1, 10, "ONGOING", false);
        saveLost112Docs();
        MATCH.respondWith(MatchResponses::respond);
        jobRunner.run(policeJob);
        assertThat(logs()).hasSize(1);

        MATCH.respondWith(request -> MatchMock.json(200, "{\"message\":\"없습니다\",\"result\":null}"));
        JobRunSummary summary = jobRunner.run(policeJob);

        assertThat(summary.status()).isEqualTo("COMPLETED");
        assertThat(logs()).isEmpty();
    }
}
