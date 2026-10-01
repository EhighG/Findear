package com.findear.batch.job;

import com.findear.batch.common.config.SchedulingConfig;
import com.findear.batch.common.job.BatchJobRunner;
import com.findear.batch.ours.job.scheduler.FindearJobScheduler;
import com.findear.batch.police.job.scheduler.PoliceJobScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.CronTask;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 스케줄 스위치(batch.scheduling.enabled)와 cron 설정값 확인. 컨테이너 없이 가벼운 컨텍스트로 확인한다 (잡·실행기는 mock).
 */
class SchedulingConfigTest {

    @Configuration
    static class JobMocks {
        @Bean
        BatchJobRunner batchJobRunner() {
            return mock(BatchJobRunner.class);
        }

        @Bean(name = "findearJob")
        Job findearJob() {
            return mock(Job.class);
        }

        @Bean(name = "policeJob")
        Job policeJob() {
            return mock(Job.class);
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(JobMocks.class, SchedulingConfig.class, FindearJobScheduler.class, PoliceJobScheduler.class)
            .withPropertyValues("batch.jobs.findear.cron=0 15 3 * * *", "batch.jobs.police.cron=0 45 5 * * *");

    @DisplayName("기본값(설정 없음)이면 스케줄링과 두 스케줄러 빈이 켜지고, cron은 설정값을 따른다")
    @Test
    void enabledByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(FindearJobScheduler.class);
            assertThat(context).hasSingleBean(PoliceJobScheduler.class);
            assertThat(context).hasSingleBean(ScheduledAnnotationBeanPostProcessor.class);

            List<String> crons = context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks().stream()
                    .map(task -> ((CronTask) task.getTask()).getExpression()).toList();
            assertThat(crons).containsExactlyInAnyOrder("0 15 3 * * *", "0 45 5 * * *");
        });
    }

    @DisplayName("batch.scheduling.enabled=true도 켜진다")
    @Test
    void enabledExplicitly() {
        runner.withPropertyValues("batch.scheduling.enabled=true").run(context -> {
            assertThat(context).hasSingleBean(FindearJobScheduler.class);
            assertThat(context).hasSingleBean(PoliceJobScheduler.class);
        });
    }

    @DisplayName("batch.scheduling.enabled=false면 스케줄링도 스케줄러 빈도 없다")
    @Test
    void disabled() {
        runner.withPropertyValues("batch.scheduling.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(FindearJobScheduler.class);
            assertThat(context).doesNotHaveBean(PoliceJobScheduler.class);
            assertThat(context).doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class);
        });
    }
}
