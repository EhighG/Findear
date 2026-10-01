package com.findear.batch.police.job.scheduler;

import com.findear.batch.common.job.BatchJobRunner;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * policeJob 스케줄. cron은 설정 {@code batch.jobs.police.cron}(POLICE_JOB_CRON).
 * 이전 실행이 아직 돌고 있으면 건너뛰고, 결과는 {@link BatchJobRunner}가 INFO 한 줄로 남긴다.
 */
@Component
@ConditionalOnProperty(prefix = "batch.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PoliceJobScheduler {

    private final BatchJobRunner jobRunner;
    private final Job policeJob;

    public PoliceJobScheduler(BatchJobRunner jobRunner, @Qualifier("policeJob") Job policeJob) {
        this.jobRunner = jobRunner;
        this.policeJob = policeJob;
    }

    @Scheduled(cron = "${batch.jobs.police.cron}")
    public void jobScheduled() {

        jobRunner.runScheduled(policeJob);
    }
}
