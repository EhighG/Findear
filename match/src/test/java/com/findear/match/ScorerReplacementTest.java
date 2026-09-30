package com.findear.match;

import com.fasterxml.jackson.databind.JsonNode;
import com.findear.match.scorer.DeterministicMatchingScorer;
import com.findear.match.scorer.MatchingScorer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static com.findear.match.ApiTestSupport.fixture;
import static com.findear.match.ApiTestSupport.postJson;
import static org.assertj.core.api.Assertions.assertThat;

/** 사용자가 MatchingScorer 빈을 등록하면 기본 구현이 빠지고 그 점수가 쓰인다. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ScorerReplacementTest.FixedScorerConfig.class)
class ScorerReplacementTest {

    @TestConfiguration
    static class FixedScorerConfig {
        @Bean
        MatchingScorer fixedScorer() {
            return (lost, candidate) -> 0.5;
        }
    }

    @Autowired
    ApplicationContext context;
    @Autowired
    MockMvc mvc;

    @Test
    void 기본_구현_빈이_없다() {
        assertThat(context.getBeansOfType(MatchingScorer.class)).hasSize(1).containsKey("fixedScorer");
        assertThat(context.getBeansOfType(DeterministicMatchingScorer.class)).isEmpty();
    }

    @Test
    void 점수가_전부_교체_구현의_값이고_동점은_입력_순서를_유지한다() throws Exception {
        JsonNode findear = postJson(mvc, "/matching/findear", fixture("matching-findear-request.json")).get("result");
        List<Long> ids = new ArrayList<>();
        for (JsonNode item : findear) {
            assertThat(item.get("similarityRate").asDouble()).isEqualTo(0.5);
            ids.add(item.get("acquiredBoardId").asLong());
        }
        assertThat(ids).containsExactly(11L, 12L, 13L, 14L, 15L);

        JsonNode lost = postJson(mvc, "/matching/lost", fixture("matching-lost-request.json")).get("result");
        List<Long> lostIds = new ArrayList<>();
        for (JsonNode item : lost) {
            assertThat(item.get("similarityRate").asDouble()).isEqualTo(0.5);
            lostIds.add(item.get("acquiredBoardId").asLong());
        }
        assertThat(lostIds).containsExactly(5001L, 5002L, 5003L, 5004L);
    }
}
