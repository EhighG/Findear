package com.findear.match.scorer;

import com.findear.match.service.Hashing;

/**
 * 기본 점수 구현. 같은 (seed, lostBoardId, candidateKey)이면 항상 같은 점수를 낸다.
 *
 * <pre>
 * r   = rand(seed, lostBoardId, candidateKey)  in [0,1)
 * raw = 0.3 + 0.6 * r + (카테고리 일치 0.1) + (색상 일치 0.05)
 * </pre>
 * 일치 판단은 양쪽이 모두 비어 있지 않고 앞뒤 공백을 제거한 값이 같을 때만 한다.
 */
public class DeterministicMatchingScorer implements MatchingScorer {

    private static final double BASE = 0.3;
    private static final double RANDOM_WEIGHT = 0.6;
    static final double CATEGORY_BONUS = 0.1;
    static final double COLOR_BONUS = 0.05;

    private final long seed;

    public DeterministicMatchingScorer(long seed) {
        this.seed = seed;
    }

    @Override
    public double score(MatchingSubject lost, MatchingSubject candidate) {
        double raw = BASE + RANDOM_WEIGHT * rand(lost.key(), candidate.key());
        if (sameNonBlank(lost.category(), candidate.category())) {
            raw += CATEGORY_BONUS;
        }
        if (sameNonBlank(lost.color(), candidate.color())) {
            raw += COLOR_BONUS;
        }
        return raw;
    }

    /** SHA-256("seed|lostBoardId|candidateKey")의 앞 8바이트(big-endian long)의 상위 53비트를 [0,1) 실수로 만든다. */
    double rand(String lostKey, String candidateKey) {
        long v = Hashing.firstLong(Hashing.sha256(seed + "|" + lostKey + "|" + candidateKey));
        return (v >>> 11) * 0x1.0p-53;
    }

    private static boolean sameNonBlank(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        String x = a.strip();
        String y = b.strip();
        return !x.isEmpty() && x.equals(y);
    }
}
