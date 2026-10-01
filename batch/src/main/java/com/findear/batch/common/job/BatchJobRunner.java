package com.findear.batch.common.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 잡을 실행하는 유일한 곳. 스케줄러와 수동 실행 API가 모두 이것을 쓴다.
 * <ul>
 *   <li>실행마다 {@code date} 파라미터(시각)가 달라 같은 잡을 다시 실행할 수 있다.</li>
 *   <li>같은 잡은 동시에 하나만 돈다. Spring Batch는 파라미터가 다르면 같은 잡을 동시에 시작해 주므로 여기서 막는다
 *       (이 앱 한 개 안에서만 유효. batch는 한 대만 띄운다). 이미 돌고 있으면 {@link BatchJobBusyException}.</li>
 *   <li>기본 JobLauncher는 잡이 끝날 때까지 기다렸다가 돌아오므로 {@link #run}은 끝난 실행의 요약을 돌려준다.</li>
 *   <li>시작에 실패하면(DB 오류 등) 예외가 나가고 다시 시도하지 않는다: 스케줄 실행은 WARN 한 줄 후 다음 cron에서 다시, API는 오류 응답.
 *       (동기 런처의 run은 잡 전체를 포함하므로 재시도하면 잡이 두 번 돈다. 두 잡이 같은 순간 시작할 때의 교착은
 *       설정 spring.batch.jdbc.isolation-level-for-create=read_committed로 없앴다.)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchJobRunner {

    private static final DateTimeFormatter PARAMETER_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS");

    private final JobLauncher jobLauncher;
    private final Set<String> running = ConcurrentHashMap.newKeySet();

    /**
     * 잡을 실행하고 끝나면 요약을 돌려준다. 잡이 FAILED로 끝나도 예외가 아니라 요약(status=FAILED)으로 돌려준다.
     *
     * @throws BatchJobBusyException 같은 잡이 이미 실행 중일 때
     */
    public JobRunSummary run(Job job) {

        String jobName = job.getName();
        if (!running.add(jobName)) {
            throw new BatchJobBusyException(jobName);
        }

        try {
            // 실행마다 다른 파라미터
            JobParameters parameters = new JobParametersBuilder()
                    .addString("date", LocalDateTime.now().format(PARAMETER_FORMAT))
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(job, parameters);

            JobRunSummary summary = JobRunSummary.of(execution);
            log.info(summary.oneLine());
            if (summary.hasFailedStep()) {
                log.warn("{}: 실패한 스텝이 있습니다. 상세는 BATCH_STEP_EXECUTION 기록과 위 로그를 보세요", jobName);
            }
            return summary;

        } catch (JobExecutionAlreadyRunningException e) {
            throw new BatchJobBusyException(jobName);
        } catch (Exception e) {
            // 시작 실패(DB 오류 등)와 파라미터·재시작 관련 예외. 다시 시도하지 않는다
            throw new IllegalStateException(jobName + " 시작 실패: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        } finally {
            running.remove(jobName);
        }
    }

    /**
     * 스케줄러용: 이미 돌고 있으면 WARN 한 줄로 건너뛰고, 예외가 나도 스케줄러 스레드로 퍼지지 않게 WARN 한 줄만 남긴다.
     * 잡 결과 INFO는 {@link #run}이 남긴다.
     */
    public void runScheduled(Job job) {

        try {
            run(job);
        } catch (BatchJobBusyException e) {
            log.warn("{} 이전 실행이 아직 끝나지 않아 이번 스케줄 실행은 건너뜁니다", job.getName());
        } catch (RuntimeException e) {
            log.warn("{} 스케줄 실행 실패: {}", job.getName(), e.getMessage());
        }
    }
}
