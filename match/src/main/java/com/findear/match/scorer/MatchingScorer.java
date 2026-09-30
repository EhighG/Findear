package com.findear.match.scorer;

/**
 * 분실물과 후보 습득물 하나의 매칭 점수를 계산하는 확장 지점 (O-1: 실제 점수 규칙은 나중에 설계한다).
 *
 * <p>구현체는 <b>원점수(double)</b>만 돌려준다. [0,1] 자르기, 소수 5자리 반올림, 내림차순 정렬(같은 점수는 입력 순서 유지),
 * 결과 수 상한은 서비스가 공통으로 처리한다. 후보가 Findear 습득물인지 Lost112 습득물인지는
 * {@link MatchingSubject}의 필드 대응으로 이미 같은 모양이 되어 있다.
 *
 * <h2>교체 방법</h2>
 * 이 인터페이스를 구현한 클래스를 <b>{@code @Component}로 등록</b>(또는 {@code @Bean} 하나 등록)하면 된다.
 * 기본 구현({@link DeterministicMatchingScorer})은 자동 구성({@code DefaultScorerAutoConfiguration})에서
 * {@code @ConditionalOnMissingBean(MatchingScorer.class)}로 등록돼 있어, 사용자 빈이 있으면 자동으로 빠진다
 * ({@code @Component}든 {@code @Bean}이든 상관없다). 구현체는 스레드 안전해야 한다(요청마다 동시에 호출된다).
 */
public interface MatchingScorer {

    /**
     * @param lost      분실물 (key = lostBoardId)
     * @param candidate 후보 습득물
     * @return 원점수. 범위는 자유롭다(서비스가 [0,1]로 자른다). NaN은 0으로 취급한다
     */
    double score(MatchingSubject lost, MatchingSubject candidate);
}
