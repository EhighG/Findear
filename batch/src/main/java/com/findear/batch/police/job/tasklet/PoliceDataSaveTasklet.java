package com.findear.batch.police.job.tasklet;

import com.findear.batch.police.service.Lost112CollectResult;
import com.findear.batch.police.service.Lost112CollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * Lost112 수집 스텝. 수집 로직은 {@link Lost112CollectService}가 하고 여기서는 부르기만 한다.
 * 키가 없거나 모든 서비스가 실패해 하나도 못 넣었으면 예외로 스텝이 실패한다. (잡 구성·켜기/끄기는 R-34)
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class PoliceDataSaveTasklet implements Tasklet, StepExecutionListener {

    private final Lost112CollectService collectService;

    @Override
    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        log.info("경찰청 데이터 저장 Start!");
    }

    @Override
    @AfterStep
    public ExitStatus afterStep(StepExecution stepExecution) {

        log.info("경찰청 데이터 저장 End!");

        return ExitStatus.COMPLETED;
    }

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {

        Lost112CollectResult result = collectService.collectOrThrow();
        log.info("Lost112 수집 완료: indexed={}, 실패={}", result.totalIndexed(), result.hasFailure() ? result.failureSummary() : "없음");

        return RepeatStatus.FINISHED;
    }
}
