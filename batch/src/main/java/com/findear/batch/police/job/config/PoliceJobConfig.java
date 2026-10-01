package com.findear.batch.police.job.config;

import com.findear.batch.police.job.tasklet.PoliceDataMatcingTasklet;
import com.findear.batch.police.job.tasklet.PoliceDataSaveTasklet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class PoliceJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PoliceDataSaveTasklet policeDataSaveTasklet;
    private final PoliceDataMatcingTasklet policeDataMatcingTasklet;

    @Bean
    public Job policeJob() {

        return new JobBuilder("policeJob", jobRepository)
//                .start(policeSaveStep())
//                .next(policeMatchingStep())
                .start(policeMatchingStep())
                .build();
    }

    @Bean
    public Step policeSaveStep() {

        return new StepBuilder("policeSaveStep", jobRepository)
                .tasklet(policeDataSaveTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step policeMatchingStep() {

        return new StepBuilder("policeMatchingStep", jobRepository)
                .tasklet(policeDataMatcingTasklet, transactionManager)
                .build();
    }

}
