package com.findear.batch.ours.job.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@ConditionalOnProperty(prefix = "batch.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class FindearJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job findearJob;

    public FindearJobScheduler(JobLauncher jobLauncher, @Qualifier("findearJob") Job findearJob) {
        this.jobLauncher = jobLauncher;
        this.findearJob = findearJob;
    }

    @Scheduled(cron = "${batch.jobs.findear.cron}")
    public void jobSchduled() throws JobParametersInvalidException, JobExecutionAlreadyRunningException,
            JobRestartException, JobInstanceAlreadyCompleteException {

        log.info("findear job schedule 실행");

        // 실행마다 다른 파라미터를 줘야 같은 잡을 다시 실행할 수 있다
        JobParameters parameters = new JobParametersBuilder()
                .addString("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS")))
                .toJobParameters();

        // 기본 JobLauncher는 잡이 끝날 때까지 기다렸다가 돌아온다
        JobExecution jobExecution = jobLauncher.run(findearJob, parameters);

        log.info("Job Execution: " + jobExecution.getStatus());
        log.info("Job getJobId: " + jobExecution.getJobId());
        log.info("Job getExitStatus: " + jobExecution.getExitStatus());
        log.info("Job getJobInstance: " + jobExecution.getJobInstance());
        log.info("Job getStepExecutions: " + jobExecution.getStepExecutions());
        log.info("Job getLastUpdated: " + jobExecution.getLastUpdated());
        log.info("Job getFailureExceptions: " + jobExecution.getFailureExceptions());

    }
}
