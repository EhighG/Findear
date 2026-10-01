package com.findear.batch.ours.job.tasklet;

import com.findear.batch.common.job.MatchingStepFailedException;
import com.findear.batch.ours.domain.LostBoard;
import com.findear.batch.ours.dto.LostBoardMatchingDto;
import com.findear.batch.ours.repository.LostBoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.NestedExceptionUtils;

import java.util.List;

/**
 * 진행 중(ONGOING)이고 삭제되지 않은 분실물마다 매칭을 하나씩 수행하는 스텝의 공통 틀 (findearJob, policeJob의 매칭 스텝).
 * <ul>
 *   <li>분실물 하나의 실패(match 오류 등)는 WARN 한 줄(분실물 id·원인 요약, 스택 없음)을 남기고 다음 분실물로 넘어간다.
 *       실패한 분실물의 기존 로그는 그대로 남는다 (로그를 쓰는 {@code MatchingLogWriter}를 부르기 전에 실패하므로).</li>
 *   <li>스텝 결과는 StepExecution 카운트로 남는다: read = 시도, write = 성공, processSkip = 실패. 끝에 INFO 한 줄.</li>
 *   <li>시도한 분실물이 1건 이상인데 모두 실패하면 스텝이 FAILED다 (match가 내려간 경우 등). 대상이 0건이면 COMPLETED.</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public abstract class LostBoardMatchingTasklet implements Tasklet {

    private static final int REASON_LIMIT = 200;

    private final LostBoardRepository lostBoardRepository;

    /** 로그에 쓰는 매칭 종류 이름 (예: findear, lost112) */
    protected abstract String kind();

    /** 분실물 하나를 매칭하고 로그를 저장한다. 실패는 예외로 알린다 */
    protected abstract void matchOne(LostBoardMatchingDto lostBoard);

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        List<LostBoard> targets = lostBoardRepository.findAllWithBoardByStatusOngoing();

        int succeeded = 0;
        int failed = 0;

        for (LostBoard lostBoard : targets) {

            Long lostBoardId = lostBoard.getId();
            contribution.incrementReadCount();

            try {
                matchOne(LostBoardMatchingDto.from(lostBoard));
                contribution.incrementWriteCount(1);
                succeeded++;
            } catch (Exception e) {
                contribution.incrementProcessSkipCount();
                failed++;
                log.warn("{} 매칭 실패, 이 분실물은 건너뜀 (lostBoardId={}): {}", kind(), lostBoardId, reason(e));
            }
        }

        log.info("{} 매칭 스텝 결과: 대상 {}건, 성공 {}건, 실패 {}건", kind(), targets.size(), succeeded, failed);

        if (!targets.isEmpty() && succeeded == 0) {
            throw new MatchingStepFailedException(kind() + " 매칭이 대상 분실물 " + targets.size() + "건 모두 실패했습니다");
        }
        return RepeatStatus.FINISHED;
    }

    /** 원인 요약 한 줄: 예외 종류와 메시지, 연결 오류처럼 메시지가 null로 끝나는 경우를 위해 가장 안쪽 원인도 붙인다 */
    private static String reason(Exception e) {
        String text = e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
        Throwable root = NestedExceptionUtils.getMostSpecificCause(e);
        if (root != e) {
            text += " (원인: " + root.getClass().getSimpleName() + (root.getMessage() == null ? "" : ": " + root.getMessage()) + ")";
        }
        return text.length() > REASON_LIMIT ? text.substring(0, REASON_LIMIT) + "..." : text;
    }
}
