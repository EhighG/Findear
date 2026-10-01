package com.findear.batch.common.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch 5.2의 `spring.batch.job.active` 중복 등록을 막는다 (Prometheus WARN 방지).
 *
 * <p>Spring Batch 5.2는 같은 이름의 장기 실행 타이머(LongTaskTimer) `spring.batch.job.active`를 두 곳에서 서로 다른 태그 키로 등록한다.
 * <ul>
 *   <li>{@code AbstractJob.execute}가 직접 만드는 것: 태그 키 {@code spring.batch.job.active.name}</li>
 *   <li>Observation 핸들러(DefaultMeterObservationHandler)가 `spring.batch.job` 관측에서 만드는 것: 태그 키 {@code spring.batch.job.name}, {@code spring.batch.job.status}</li>
 * </ul>
 * Prometheus는 같은 이름의 지표가 같은 태그 키를 가져야 해서, 잡을 처음 실행할 때 뒤에 등록되는 쪽이 거부되고 WARN이 한 번 남는다.
 * 거부되는 쪽(관측 기반)은 지금도 노출되지 않으므로 그쪽만 미리 막는다. 노출되는 `spring_batch_job_active_seconds_*{spring_batch_job_active_name}`은 그대로다.
 *
 * <p>Spring Batch 이슈 #4753(6.0.0-M4에서 Observation API로 일원화해 해결, 5.2.x 백포트는 확인 못 함 — 사용 버전 5.2.6에서 재현)에서 제안된 MeterFilter 우회책을, 거부되는 쪽(관측 기반)만 막도록 좁힌 것이다.
 * 이슈: https://github.com/spring-projects/spring-batch/issues/4753.
 * Boot는 MeterFilter 빈을 모든 MeterRegistry에 적용한다 (https://docs.spring.io/spring-boot/3.5/reference/actuator/metrics.html#actuator.metrics.customizing).
 * Batch 6(Boot 4)으로 올리면 이 설정은 지운다.
 */
@Configuration
public class BatchMetricsConfig {

    static final String JOB_ACTIVE = "spring.batch.job.active";
    static final String OBSERVATION_TAG_KEY = "spring.batch.job.name";

    @Bean
    public MeterFilter denyObservationBasedJobActiveTimer() {
        return MeterFilter.deny(BatchMetricsConfig::isObservationBasedJobActive);
    }

    static boolean isObservationBasedJobActive(Meter.Id id) {
        return JOB_ACTIVE.equals(id.getName()) && id.getTag(OBSERVATION_TAG_KEY) != null;
    }
}
