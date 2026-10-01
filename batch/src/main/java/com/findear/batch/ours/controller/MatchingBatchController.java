package com.findear.batch.ours.controller;

import com.findear.batch.common.job.BatchJobRunner;
import com.findear.batch.common.job.JobRunSummary;
import com.findear.batch.common.response.SuccessResponse;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 잡 수동 실행 API (내부용, 07 §3). 스케줄과 같은 잡을 같은 방식으로 실행하므로 잡 실행 기록(BATCH_JOB_EXECUTION)과 지표가 남는다.
 * 잡이 끝날 때까지 기다렸다가 실행 요약({@link JobRunSummary})을 돌려준다. 잡이 FAILED로 끝나도 200이고 요약의 status가 FAILED다.
 * 같은 잡이 이미 돌고 있으면 409.
 */
@RestController
public class MatchingBatchController {

    private final BatchJobRunner jobRunner;
    private final Job findearJob;
    private final Job policeJob;

    public MatchingBatchController(BatchJobRunner jobRunner,
                                   @Qualifier("findearJob") Job findearJob,
                                   @Qualifier("policeJob") Job policeJob) {
        this.jobRunner = jobRunner;
        this.findearJob = findearJob;
        this.policeJob = policeJob;
    }

    /** findearJob 실행: 진행 중인 분실물마다 Findear 습득물 매칭 */
    @PostMapping("/findear/matching/batch")
    public ResponseEntity<?> runFindearJob() {

        JobRunSummary summary = jobRunner.run(findearJob);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "findearJob 실행 완료 (" + summary.status() + ")", summary));
    }

    /** policeJob 실행: Lost112 수집(lost112.collect-enabled일 때만) 후 진행 중인 분실물마다 Lost112 매칭 */
    @PostMapping("/police/matching/batch")
    public ResponseEntity<?> runPoliceJob() {

        JobRunSummary summary = jobRunner.run(policeJob);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "policeJob 실행 완료 (" + summary.status() + ")", summary));
    }
}
