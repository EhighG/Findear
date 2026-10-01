package com.findear.batch.common.job;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 잡 실행 한 번의 요약 (수동 실행 API 응답 {@code result}와 스케줄러 로그에 쓴다).
 *
 * @param jobExecutionId BATCH_JOB_EXECUTION의 ID
 * @param status         잡 상태 (COMPLETED, FAILED 등)
 * @param durationMillis 실행 시간(ms)
 * @param steps          실행된 스텝별 요약 (실행 순서)
 */
public record JobRunSummary(Long jobExecutionId, String jobName, String status, String exitCode, long durationMillis,
                            List<StepSummary> steps) {

    private static final int MESSAGE_LIMIT = 300;

    /**
     * @param processed 매칭 스텝: 시도한 분실물 수
     * @param succeeded 매칭 스텝: 성공한 분실물 수 (수집 스텝: 인덱스에 넣은 문서 수)
     * @param failed    매칭 스텝: 실패해 건너뛴 분실물 수
     * @param message   스텝이 실패한 경우 원인 요약 (없으면 null)
     */
    public record StepSummary(String stepName, String status, String exitCode, long processed, long succeeded,
                              long failed, String message) {

        static StepSummary of(StepExecution step) {
            String message = step.getStatus() == BatchStatus.COMPLETED || step.getFailureExceptions().isEmpty()
                    ? null
                    : abbreviate(step.getFailureExceptions().get(0));
            return new StepSummary(step.getStepName(), step.getStatus().name(), step.getExitStatus().getExitCode(),
                    step.getReadCount(), step.getWriteCount(), step.getProcessSkipCount(), message);
        }

        private static String abbreviate(Throwable failure) {
            String text = failure.getClass().getSimpleName() + (failure.getMessage() == null ? "" : ": " + failure.getMessage());
            return text.length() > MESSAGE_LIMIT ? text.substring(0, MESSAGE_LIMIT) + "..." : text;
        }

        String oneLine() {
            return stepName + " " + status + " (처리 " + processed + ", 성공 " + succeeded + ", 실패 " + failed + ")"
                    + (message == null ? "" : " [" + message + "]");
        }
    }

    public static JobRunSummary of(JobExecution execution) {

        LocalDateTime start = execution.getStartTime();
        LocalDateTime end = execution.getEndTime() == null ? LocalDateTime.now() : execution.getEndTime();
        long millis = start == null ? 0 : Duration.between(start, end).toMillis();

        return new JobRunSummary(execution.getId(), execution.getJobInstance().getJobName(), execution.getStatus().name(),
                execution.getExitStatus().getExitCode(), millis,
                execution.getStepExecutions().stream().map(StepSummary::of).toList());
    }

    /** 로그용 한 줄 */
    public String oneLine() {
        return jobName + " 실행 결과: " + status + " (jobExecutionId=" + jobExecutionId + ", " + durationMillis + "ms) 스텝: "
                + String.join(" / ", steps.stream().map(StepSummary::oneLine).toList());
    }

    public boolean hasFailedStep() {
        return steps.stream().anyMatch(s -> !BatchStatus.COMPLETED.name().equals(s.status()));
    }
}
