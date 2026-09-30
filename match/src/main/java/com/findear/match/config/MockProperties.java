package com.findear.match.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * mock 동작 설정 (환경변수 MATCH_MOCK_SEED, MATCH_MOCK_LATENCY_MS, MATCH_MOCK_MAX_RESULTS).
 * 범위를 벗어나면 기동이 실패한다.
 *
 * @param seed       결정적 선택·점수에 섞는 값. 바꾸면 결과가 달라진다
 * @param latencyMs  POST 엔드포인트 응답 전 대기 시간(ms). 0이면 대기 없음 (모니터링 확인용)
 * @param maxResults 매칭 응답의 최대 결과 수
 */
@Validated
@ConfigurationProperties("match.mock")
public record MockProperties(
        long seed,
        @Min(0) long latencyMs,
        @Min(1) int maxResults) {
}
