package com.findear.match.config;

import com.findear.match.scorer.DeterministicMatchingScorer;
import com.findear.match.scorer.MatchingScorer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 기본 점수 구현 등록. 사용자 설정이 모두 처리된 뒤에 평가되도록 자동 구성으로 등록했다
 * ({@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}).
 * 일반 {@code @Configuration}에 두면 사용자 빈보다 먼저 평가돼 {@code @ConditionalOnMissingBean}이 동작하지 않을 수 있다.
 * 사용자가 {@link MatchingScorer} 빈을 하나 등록하면 이 기본 구현은 빠진다.
 */
@AutoConfiguration
public class DefaultScorerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MatchingScorer.class)
    MatchingScorer deterministicMatchingScorer(MockProperties properties) {
        return new DeterministicMatchingScorer(properties.seed());
    }
}
