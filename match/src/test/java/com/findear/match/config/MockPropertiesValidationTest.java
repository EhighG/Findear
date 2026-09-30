package com.findear.match.config;

import com.findear.match.scorer.MatchingScorer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class MockPropertiesValidationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(MatchConfig.class)
            .withConfiguration(AutoConfigurations.of(DefaultScorerAutoConfiguration.class));

    @Test
    void 유효한_값이면_기동한다() {
        runner.withPropertyValues("match.mock.seed=1", "match.mock.latency-ms=0", "match.mock.max-results=1")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(MockProperties.class).maxResults()).isEqualTo(1);
                    assertThat(context).hasSingleBean(MatchingScorer.class);
                });
    }

    @Test
    void max_results가_0이면_기동_실패() {
        runner.withPropertyValues("match.mock.seed=1", "match.mock.latency-ms=0", "match.mock.max-results=0")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void latency_ms가_음수면_기동_실패() {
        runner.withPropertyValues("match.mock.seed=1", "match.mock.latency-ms=-1", "match.mock.max-results=100")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void 숫자가_아닌_값이면_기동_실패() {
        runner.withPropertyValues("match.mock.seed=abc", "match.mock.latency-ms=0", "match.mock.max-results=100")
                .run(context -> assertThat(context).hasFailed());
    }
}
