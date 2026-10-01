package com.findear.batch.ours.job.scheduler;

import com.findear.batch.common.job.BatchJobRunner;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * findearJob 스케줄. cron은 설정 {@code batch.jobs.findear.cron}(FINDEAR_JOB_CRON).
 * 이전 실행이 아직 돌고 있으면 건너뛰고, 결과는 {@link BatchJobRunner}가 INFO 한 줄로 남긴다.
 */
@Component
@ConditionalOnProperty(prefix = "batch.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FindearJobScheduler {

    private final BatchJobRunner jobRunner;
    private final Job findearJob;

    public FindearJobScheduler(BatchJobRunner jobRunner, @Qualifier("findearJob") Job findearJob) {
        this.jobRunner = jobRunner;
        this.findearJob = findearJob;
    }

    @Scheduled(cron = "${batch.jobs.findear.cron}")
    public void jobScheduled() {

        jobRunner.runScheduled(findearJob);
    }
}
