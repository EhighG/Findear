package com.findear.batch.ours.job.config;

import com.findear.batch.ours.job.tasklet.FindearDataMatchingTasklet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FindearJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final FindearDataMatchingTasklet findearDataMatchingTasklet;

    @Bean
    public Job FindearJob() {

        Job findearJob = jobBuilderFactory.get("findearJob")
                .start(findearMatchingStep())
                .build();

        return findearJob;
    }

    @Bean
    public Step findearMatchingStep() {

        return stepBuilderFactory.get("findearMatchingStep")
                .tasklet(findearDataMatchingTasklet)
                .build();
    }
}