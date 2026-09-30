package com.findear.match;

import com.findear.match.scorer.DeterministicMatchingScorer;
import com.findear.match.scorer.MatchingScorer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MatchApplicationTests {

    @Autowired
    MatchingScorer scorer;

    @Test
    void 기본_설정에서_기본_점수_구현이_등록된다() {
        assertThat(scorer).isInstanceOf(DeterministicMatchingScorer.class);
    }
}
