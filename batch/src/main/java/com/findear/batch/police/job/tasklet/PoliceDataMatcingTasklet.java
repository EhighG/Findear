package com.findear.batch.police.job.tasklet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PoliceDataMatcingTasklet implements Tasklet, StepExecutionListener {
    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

        // matching 로직


        return RepeatStatus.FINISHED;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {

        log.info("매칭 이전 start");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        log.info("매칭 이전 start");

        return ExitStatus.COMPLETED;
    }
}
