package com.findear.batch.common.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Batch 5.2의 spring.batch.job.active 중복 등록(이슈 #4753) 방지 확인.
 * AbstractJob이 만드는 타이머(태그 spring.batch.job.active.name)와 관측 핸들러가 만드는 타이머(spring.batch.job.name·status)를
 * 같은 이름으로 등록해 Prometheus 레지스트리가 WARN을 남기는 상황을 재현한다. DB·Docker는 쓰지 않는다.
 */
class BatchMetricsConfigTest {

    private static void registerAbstractJobTimer(PrometheusMeterRegistry registry) {
        LongTaskTimer.builder("spring.batch.job.active").description("Active jobs")
                .tag("spring.batch.job.active.name", "policeJob").register(registry);
    }

    private static void registerObservationTimer(PrometheusMeterRegistry registry) {
        LongTaskTimer.builder("spring.batch.job.active")
                .tag("spring.batch.job.name", "policeJob").tag("spring.batch.job.status", "UNKNOWN").register(registry);
    }

    /** 같은 레지스트리 구현이 남기는 로그(WARN)를 모은다 */
    private static ListAppender<ILoggingEvent> captureWarnings() {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        ((Logger) LoggerFactory.getLogger("io.micrometer")).addAppender(appender);
        return appender;
    }

    private static List<String> warnings(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream()
                .filter(event -> event.getLevel().toInt() >= ch.qos.logback.classic.Level.WARN.toInt())
                .map(ILoggingEvent::getFormattedMessage).toList();
    }

    @DisplayName("재현: 필터가 없으면 두 번째 등록이 거부되고 WARN이 남는다")
    @Test
    void withoutFilterWarns() {
        ListAppender<ILoggingEvent> appender = captureWarnings();
        try {
            PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

            registerAbstractJobTimer(registry);
            registerObservationTimer(registry);

            assertThat(warnings(appender)).anyMatch(message -> message.contains("spring.batch.job.active"));
        } finally {
            ((Logger) LoggerFactory.getLogger("io.micrometer")).detachAppender(appender);
        }
    }

    @DisplayName("필터가 있으면 WARN이 없고, AbstractJob 쪽 spring_batch_job_active_seconds는 그대로 노출된다 (어느 쪽이 먼저 등록돼도)")
    @Test
    void withFilterNoWarnAndMetricKept() {
        for (boolean abstractJobFirst : new boolean[]{true, false}) {
            ListAppender<ILoggingEvent> appender = captureWarnings();
            try {
                PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
                registry.config().meterFilter(new BatchMetricsConfig().denyObservationBasedJobActiveTimer());

                if (abstractJobFirst) {
                    registerAbstractJobTimer(registry);
                    registerObservationTimer(registry);
                } else {
                    registerObservationTimer(registry);
                    registerAbstractJobTimer(registry);
                }

                assertThat(warnings(appender)).isEmpty();
                String scrape = registry.scrape();
                assertThat(scrape).contains("spring_batch_job_active_seconds_count{spring_batch_job_active_name=\"policeJob\"}");
                assertThat(scrape).doesNotContain("spring_batch_job_name=\"policeJob\"");
            } finally {
                ((Logger) LoggerFactory.getLogger("io.micrometer")).detachAppender(appender);
            }
        }
    }

    @DisplayName("필터는 spring.batch.job.active의 관측 기반 타이머만 막고 다른 지표는 막지 않는다")
    @Test
    void onlyObservationBasedTimerDenied() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.config().meterFilter(new BatchMetricsConfig().denyObservationBasedJobActiveTimer());

        LongTaskTimer.builder("spring.batch.step.active").tag("spring.batch.step.name", "s").register(registry);
        io.micrometer.core.instrument.Timer.builder("spring.batch.job").tag("spring.batch.job.name", "policeJob")
                .tag("spring.batch.job.status", "COMPLETED").register(registry).record(java.time.Duration.ofMillis(5));
        registry.counter("spring.batch.job.launch.count").increment();

        String scrape = registry.scrape();
        assertThat(scrape).contains("spring_batch_step_active_seconds", "spring_batch_job_seconds_count", "spring_batch_job_launch_count_total");
    }
}
