package com.findear.batch.police.job.config;

import com.findear.batch.police.job.tasklet.PoliceDataMatchingTasklet;
import com.findear.batch.police.job.tasklet.PoliceDataSaveTasklet;
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
public class PoliceJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PoliceDataSaveTasklet policeDataSaveTasklet;
    private final PoliceDataMatchingTasklet policeDataMatchingTasklet;

    /**
     * policeSaveStep(Lost112 수집, lost112.collect-enabled일 때만) -> policeMatchingStep(Lost112 매칭).
     * 수집 스텝이 어떤 결과로 끝나도(FAILED 포함) 매칭 스텝은 실행한다: 수집이 실패해도 이미 쌓인 Lost112 데이터로 매칭할 수 있다.
     * 수집 스텝의 실패는 그 스텝의 상태(FAILED)와 로그에 남고, 잡의 최종 상태는 매칭 스텝 결과를 따른다.
     */
    @Bean
    public Job policeJob() {

        return new JobBuilder("policeJob", jobRepository)
                .start(policeSaveStep())
                .on("*").to(policeMatchingStep())
                .end()
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
                .tasklet(policeDataMatchingTasklet, transactionManager)
                .build();
    }

}
