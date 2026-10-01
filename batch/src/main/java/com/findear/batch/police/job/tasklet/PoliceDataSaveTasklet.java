package com.findear.batch.police.job.tasklet;

import com.findear.batch.police.client.Lost112Properties;
import com.findear.batch.police.service.Lost112CollectResult;
import com.findear.batch.police.service.Lost112CollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

/**
 * Lost112 수집 스텝 (policeJob의 첫 스텝). 수집 로직은 {@link Lost112CollectService}가 하고 여기서는 부르기만 한다.
 * <ul>
 *   <li>{@code lost112.collect-enabled}(LOST112_COLLECT_ENABLED, 기본 false)가 false면 수집하지 않고 INFO 한 줄 후 COMPLETED.</li>
 *   <li>true인데 키가 없거나({@code Lost112NotConfiguredException}) 모든 서비스가 실패해 하나도 못 넣었으면
 *       ({@code Lost112UnavailableException}) 예외로 스텝이 FAILED다. 일부 서비스만 실패하면 COMPLETED이고 WARN 한 줄을 남긴다.</li>
 *   <li>스텝이 실패해도 다음 매칭 스텝은 실행된다 (PoliceJobConfig).</li>
 * </ul>
 * 수집한(인덱스에 넣은) 문서 수는 스텝의 write 카운트에 남는다.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class PoliceDataSaveTasklet implements Tasklet {

    private final Lost112CollectService collectService;
    private final Lost112Properties properties;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        if (!properties.isCollectEnabled()) {
            log.info("Lost112 수집이 꺼져 있어 건너뜁니다 (lost112.collect-enabled=false, 켜려면 LOST112_COLLECT_ENABLED=true)");
            return RepeatStatus.FINISHED;
        }

        Lost112CollectResult result = collectService.collectOrThrow();
        contribution.incrementWriteCount(result.totalIndexed());
        if (result.hasFailure()) {
            log.warn("Lost112 수집 완료 (일부 서비스 실패): indexed={}, 실패={}", result.totalIndexed(), result.failureSummary());
        } else {
            log.info("Lost112 수집 완료: indexed={}", result.totalIndexed());
        }

        return RepeatStatus.FINISHED;
    }
}
