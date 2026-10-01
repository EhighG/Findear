package com.findear.batch.job;

import com.findear.batch.common.config.SchedulingConfig;
import com.findear.batch.common.job.BatchJobBusyException;
import com.findear.batch.common.job.BatchJobRunner;
import com.findear.batch.common.job.JobRunSummary;
import com.findear.batch.ours.job.scheduler.FindearJobScheduler;
import com.findear.batch.police.job.scheduler.PoliceJobScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

/**
 * BatchJobRunner(잡 실행기)와 스케줄러 연결 확인. 컨테이너 없이 가볍게 확인한다 (런처는 mock).
 * 실제 잡을 DB에서 돌리는 확인은 FindearJobTest·PoliceJobTest·MatchingBatchApiTest·ScheduledJobsTest.
 */
class BatchJobRunnerTest {

    private static Job job(String name) {
        Job job = mock(Job.class);
        when(job.getName()).thenReturn(name);
        return job;
    }

    private static JobExecution completed(String name) {
        JobExecution execution = new JobExecution(new JobInstance(1L, name), 5L, new JobParameters());
        execution.setStatus(BatchStatus.COMPLETED);
        execution.setExitStatus(ExitStatus.COMPLETED);
        execution.setStartTime(LocalDateTime.now().minusSeconds(2));
        execution.setEndTime(LocalDateTime.now());
        return execution;
    }

    @DisplayName("run은 끝난 실행의 요약을 돌려주고, 실행마다 date 파라미터가 다르다")
    @Test
    void runReturnsSummary() throws Exception {
        JobLauncher launcher = mock(JobLauncher.class);
        Job findearJob = job("findearJob");
        when(launcher.run(any(), any())).thenReturn(completed("findearJob"));
        BatchJobRunner runner = new BatchJobRunner(launcher);

        JobRunSummary summary = runner.run(findearJob);

        assertThat(summary.jobExecutionId()).isEqualTo(5L);
        assertThat(summary.jobName()).isEqualTo("findearJob");
        assertThat(summary.status()).isEqualTo("COMPLETED");
        assertThat(summary.durationMillis()).isGreaterThanOrEqualTo(2000);
        assertThat(summary.steps()).isEmpty();

        runner.run(findearJob);
        org.mockito.ArgumentCaptor<JobParameters> captor = org.mockito.ArgumentCaptor.forClass(JobParameters.class);
        verify(launcher, times(2)).run(any(), captor.capture());
        assertThat(captor.getAllValues().get(0).getString("date")).isNotNull();
    }

    @DisplayName("같은 잡이 실행 중이면 BatchJobBusyException, 스케줄 실행은 건너뛰고(런처 호출 없음), 다른 잡은 막지 않으며, 끝나면 다시 실행된다")
    @Test
    void overlappingRun() throws Exception {
        JobLauncher launcher = mock(JobLauncher.class);
        Job findearJob = job("findearJob");
        Job policeJob = job("policeJob");
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger launches = new AtomicInteger();
        when(launcher.run(any(), any())).thenAnswer(invocation -> {
            launches.incrementAndGet();
            Job launched = invocation.getArgument(0);
            if (launched.getName().equals("findearJob")) {
                release.await(30, TimeUnit.SECONDS);
            }
            return completed(launched.getName());
        });
        BatchJobRunner runner = new BatchJobRunner(launcher);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<JobRunSummary> first = executor.submit(() -> runner.run(findearJob));
            await().atMost(10, TimeUnit.SECONDS).until(() -> launches.get() == 1);

            assertThatThrownBy(() -> runner.run(findearJob)).isInstanceOf(BatchJobBusyException.class).hasMessageContaining("이미 실행 중");
            runner.runScheduled(findearJob); // 건너뜀: 예외도 새 실행도 없다
            assertThat(launches.get()).isEqualTo(1);

            runner.run(policeJob); // 다른 잡은 동시에 돌 수 있다
            assertThat(launches.get()).isEqualTo(2);

            release.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS).status()).isEqualTo("COMPLETED");
        } finally {
            release.countDown();
            executor.shutdownNow();
        }

        runner.run(findearJob);
        assertThat(launches.get()).isEqualTo(3);
    }

    @DisplayName("런처가 JobExecutionAlreadyRunningException을 내면 BatchJobBusyException으로 바꾸고, 스케줄 실행은 어떤 예외도 밖으로 내지 않는다")
    @Test
    void launcherExceptions() throws Exception {
        JobLauncher launcher = mock(JobLauncher.class);
        Job findearJob = job("findearJob");
        doThrow(new JobExecutionAlreadyRunningException("running")).when(launcher).run(any(), any());
        BatchJobRunner runner = new BatchJobRunner(launcher);

        assertThatThrownBy(() -> runner.run(findearJob)).isInstanceOf(BatchJobBusyException.class);
        runner.runScheduled(findearJob);

        doThrow(new IllegalStateException("db down")).when(launcher).run(any(), any());
        assertThatThrownBy(() -> runner.run(findearJob)).isInstanceOf(IllegalStateException.class).hasMessageContaining("db down");
        runner.runScheduled(findearJob);

        // 실패 뒤에도 잠금은 풀려 있다
        doReturn(completed("findearJob")).when(launcher).run(any(), any());
        assertThat(runner.run(findearJob).status()).isEqualTo("COMPLETED");
    }

    @DisplayName("시작 실패(DB 교착 등)는 다시 시도하지 않는다: 런처는 1번만 호출되고, run은 IllegalStateException, 스케줄 실행은 예외 없이 끝나며 잠금은 풀린다")
    @Test
    void launchFailureIsNotRetried() throws Exception {
        JobLauncher launcher = mock(JobLauncher.class);
        Job findearJob = job("findearJob");
        doThrow(new CannotAcquireLockException("Deadlock found when trying to get lock")).when(launcher).run(any(), any());
        BatchJobRunner runner = new BatchJobRunner(launcher);

        assertThatThrownBy(() -> runner.run(findearJob)).isInstanceOf(IllegalStateException.class).hasMessageContaining("CannotAcquireLockException");
        verify(launcher, times(1)).run(any(), any());

        runner.runScheduled(findearJob); // WARN 한 줄, 예외 없음
        verify(launcher, times(2)).run(any(), any());

        doReturn(completed("findearJob")).when(launcher).run(any(), any());
        assertThat(runner.run(findearJob).status()).isEqualTo("COMPLETED");
    }

    @Configuration
    static class SchedulingBeans {
        static final AtomicInteger FINDEAR_LAUNCHES = new AtomicInteger();
        static final AtomicInteger POLICE_LAUNCHES = new AtomicInteger();

        @Bean
        JobLauncher jobLauncher() throws Exception {
            JobLauncher launcher = mock(JobLauncher.class);
            when(launcher.run(any(), any())).thenAnswer(invocation -> {
                Job launched = invocation.getArgument(0);
                (launched.getName().equals("findearJob") ? FINDEAR_LAUNCHES : POLICE_LAUNCHES).incrementAndGet();
                return completed(launched.getName());
            });
            return launcher;
        }

        @Bean
        BatchJobRunner batchJobRunner(JobLauncher jobLauncher) {
            return new BatchJobRunner(jobLauncher);
        }

        @Bean(name = "findearJob")
        Job findearJob() {
            return job("findearJob");
        }

        @Bean(name = "policeJob")
        Job policeJob() {
            return job("policeJob");
        }
    }

    @DisplayName("스케줄러는 cron이 오면 BatchJobRunner로 해당 잡을 실행한다 (매초 cron으로 두 잡 모두 한 번 이상)")
    @Test
    void schedulersLaunchTheirJobs() {
        SchedulingBeans.FINDEAR_LAUNCHES.set(0);
        SchedulingBeans.POLICE_LAUNCHES.set(0);

        new ApplicationContextRunner()
                .withUserConfiguration(SchedulingBeans.class, SchedulingConfig.class, FindearJobScheduler.class, PoliceJobScheduler.class)
                .withPropertyValues("batch.jobs.findear.cron=* * * * * *", "batch.jobs.police.cron=* * * * * *", "spring.task.scheduling.pool.size=2")
                .run(context -> await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
                    assertThat(SchedulingBeans.FINDEAR_LAUNCHES.get()).isGreaterThanOrEqualTo(1);
                    assertThat(SchedulingBeans.POLICE_LAUNCHES.get()).isGreaterThanOrEqualTo(1);
                }));
    }
}
