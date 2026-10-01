package com.findear.batch.ours.job.config;

import com.findear.batch.ours.job.tasklet.FindearDataMatchingTasklet;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class FindearJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final FindearDataMatchingTasklet findearDataMatchingTasklet;

    /** findearMatchingStep 하나: 진행 중인 분실물마다 Findear 습득물 매칭 */
    @Bean
    public Job findearJob() {

        return new JobBuilder("findearJob", jobRepository)
                .start(findearMatchingStep())
                .build();
    }

    @Bean
    public Step findearMatchingStep() {

        return new StepBuilder("findearMatchingStep", jobRepository)
                .tasklet(findearDataMatchingTasklet, transactionManager)
                .build();
    }
}
