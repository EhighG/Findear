package com.findear.batch.police.job.config;

import com.findear.batch.police.job.tasklet.PoliceDataMatcingTasklet;
import com.findear.batch.police.job.tasklet.PoliceDataSaveTasklet;
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
public class PoliceJobConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final PoliceDataSaveTasklet policeDataSaveTasklet;
    private final PoliceDataMatcingTasklet PoliceDataMatcingTasklet;

    @Bean
    public Job PoliceJob(){

        Job policeJob = jobBuilderFactory.get("policeJob")
//                .start(policeSaveStep())
//                .next(policeMatchingStep())
                .start(policeMatchingStep())
                .build();

        return policeJob;
    }

    @Bean
    public Step policeSaveStep(){
        return stepBuilderFactory.get("policeSaveStep")
                .tasklet(policeDataSaveTasklet)
                .build();
    }

    @Bean
    public Step policeMatchingStep(){
        return stepBuilderFactory.get("policeMatchingStep")
                .tasklet(PoliceDataMatcingTasklet)
                .build();
    }

}