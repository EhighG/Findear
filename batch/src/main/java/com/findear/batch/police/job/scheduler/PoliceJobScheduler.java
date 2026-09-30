package com.findear.batch.police.job.scheduler;

import com.findear.batch.police.job.config.PoliceJobConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Configuration
@Slf4j
public class PoliceJobScheduler {

    private final JobLauncher jobLauncher;
    private final PoliceJobConfig policeJobConfig;

    @Scheduled(cron = "0 0 4 * * *")
    public void jobSchduled() throws JobParametersInvalidException, JobExecutionAlreadyRunningException,
            JobRestartException, JobInstanceAlreadyCompleteException {

        Job policeJob = policeJobConfig.PoliceJob();

        log.info("police job schedule 실행");

        Map<String, JobParameter> jobParametersMap = new HashMap<>();

        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        Date time = new Date();

        String time1 = format1.format(time);

        jobParametersMap.put("date",new JobParameter(time1));

        JobParameters parameters = new JobParameters(jobParametersMap);

        JobExecution jobExecution = jobLauncher.run(policeJob, parameters);

        while (jobExecution.isRunning()) {
            log.info("...");
        }

        log.info("Job Execution: " + jobExecution.getStatus());
        log.info("Job getJobConfigurationName: " + jobExecution.getJobConfigurationName());
        log.info("Job getJobId: " + jobExecution.getJobId());
        log.info("Job getExitStatus: " + jobExecution.getExitStatus());
        log.info("Job getJobInstance: " + jobExecution.getJobInstance());
        log.info("Job getStepExecutions: " + jobExecution.getStepExecutions());
        log.info("Job getLastUpdated: " + jobExecution.getLastUpdated());
        log.info("Job getFailureExceptions: " + jobExecution.getFailureExceptions());

    }
}