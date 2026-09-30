package com.findear.match.scorer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DeterministicMatchingScorerTest {

    static final double EPS = 1e-12;
    final DeterministicMatchingScorer scorer = new DeterministicMatchingScorer(42);

    MatchingSubject lost(String category, String color) {
        return new MatchingSubject("101", category, color, "지갑", null);
    }

    MatchingSubject candidate(String category, String color) {
        return new MatchingSubject("11", category, color, "지갑", null);
    }

    double base() {
        return scorer.score(lost("가", "나"), candidate("다", "라"));
    }

    @Test
    void 일치하지_않으면_기본점수는_0점3에서_0점9_사이() {
        assertThat(base()).isBetween(0.3, 0.9);
    }

    @Test
    void 카테고리_일치는_0점1_색상은_0점05_둘다는_0점15_가산() {
        double base = base();
        assertThat(scorer.score(lost("지갑", "가"), candidate("지갑", "나")) - base).isCloseTo(0.1, within(EPS));
        assertThat(scorer.score(lost("가", "검정"), candidate("나", "검정")) - base).isCloseTo(0.05, within(EPS));
        assertThat(scorer.score(lost("지갑", "검정"), candidate("지갑", "검정")) - base).isCloseTo(0.15, within(EPS));
    }

    @Test
    void 한쪽이_null이거나_비어_있으면_가산_없음() {
        double base = base();
        assertThat(scorer.score(lost(null, null), candidate(null, null))).isEqualTo(base);
        assertThat(scorer.score(lost("지갑", "검정"), candidate(null, null))).isEqualTo(base);
        assertThat(scorer.score(lost(null, null), candidate("지갑", "검정"))).isEqualTo(base);
        assertThat(scorer.score(lost("", ""), candidate("", ""))).isEqualTo(base);
        assertThat(scorer.score(lost("  ", " "), candidate("  ", " "))).isEqualTo(base);
    }

    @Test
    void 앞뒤_공백은_무시한다() {
        double base = base();
        assertThat(scorer.score(lost(" 지갑 ", "가"), candidate("지갑", "나")) - base).isCloseTo(0.1, within(EPS));
        assertThat(scorer.score(lost("가", "검정\t"), candidate("나", "  검정")) - base).isCloseTo(0.05, within(EPS));
    }

    @Test
    void 같은_입력은_같은_점수_seed나_키가_다르면_달라진다() {
        assertThat(scorer.score(lost("가", "나"), candidate("다", "라"))).isEqualTo(base());
        assertThat(new DeterministicMatchingScorer(43).score(lost("가", "나"), candidate("다", "라"))).isNotEqualTo(base());
        MatchingSubject other = new MatchingSubject("12", "다", "라", null, null);
        assertThat(scorer.score(lost("가", "나"), other)).isNotEqualTo(base());
    }
}
