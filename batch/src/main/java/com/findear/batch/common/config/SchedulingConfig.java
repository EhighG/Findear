package com.findear.batch.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄 실행 스위치. batch.scheduling.enabled(BATCH_SCHEDULING_ENABLED)가 false면 스케줄링과 스케줄러 빈이 모두 꺼진다.
 * 값이 없으면 켜진다. 테스트·수동 실행 전용 환경에서 끈다.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "batch.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
