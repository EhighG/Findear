package com.findear.match.service;

import com.findear.match.config.MockProperties;
import com.findear.match.dto.FindearAcquiredRequest;
import com.findear.match.dto.FindearMatchItem;
import com.findear.match.dto.FindearMatchRequest;
import com.findear.match.dto.LostAcquiredRequest;
import com.findear.match.dto.LostBoardRequest;
import com.findear.match.dto.LostMatchItem;
import com.findear.match.dto.LostMatchRequest;
import com.findear.match.dto.MatchResponse;
import com.findear.match.scorer.MatchingScorer;
import com.findear.match.scorer.MatchingSubject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * 매칭 mock. 점수 계산은 {@link MatchingScorer}에 맡기고, [0,1] 자르기·소수 5자리 반올림·정렬·상한은 여기서 공통으로 한다.
 */
@Service
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    static final String FINDEAR_EMPTY = "해당 분실물과 매칭 가능한 findear 데이터가 없습니다.";
    static final String FINDEAR_DONE = "해당 분실물과 findear 데이터와의 매칭이 완료되었습니다";
    static final String LOST_EMPTY = "해당 분실물과 매칭 가능한 lost112 데이터가 없습니다.";
    static final String LOST_DONE = "해당 분실물과 lost112 데이터와의 매칭이 완료되었습니다";

    private final MatchingScorer scorer;
    private final int maxResults;

    public MatchingService(MatchingScorer scorer, MockProperties properties) {
        this.scorer = scorer;
        this.maxResults = properties.maxResults();
    }

    public MatchResponse<FindearMatchItem> matchFindear(FindearMatchRequest request) {
        long lostBoardId = parseLostBoardId(request);
        List<FindearAcquiredRequest> candidates = request.acquiredBoardList();
        if (candidates == null || candidates.isEmpty()) {
            log.info("findear 매칭: lostBoardId={}, 후보 0건 → 결과 없음", lostBoardId);
            return new MatchResponse<>(FINDEAR_EMPTY, null);
        }
        MatchingSubject lost = lostSubject(request.lostBoard(), lostBoardId);

        List<Candidate<FindearAcquiredRequest, Long>> parsed = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            FindearAcquiredRequest c = candidates.get(i);
            if (c == null) {
                throw new BadRequestException("acquiredBoardList[" + i + "]는 null일 수 없습니다.");
            }
            int index = i;
            long acquiredBoardId = parseLong(c.acquiredBoardId())
                    .orElseThrow(() -> new BadRequestException(
                            "acquiredBoardList[" + index + "].acquiredBoardId는 정수여야 합니다."));
            parsed.add(new Candidate<>(c, acquiredBoardId,
                    new MatchingSubject(String.valueOf(acquiredBoardId), c.categoryName(), c.color(),
                            c.productName(), c.description())));
        }

        List<FindearMatchItem> result = rank(lost, parsed,
                (c, rate) -> new FindearMatchItem(lostBoardId, c.parsedId(), rate));
        log.info("findear 매칭: lostBoardId={}, 후보 {}건 → 결과 {}건", lostBoardId, candidates.size(), result.size());
        return new MatchResponse<>(FINDEAR_DONE, result);
    }

    public MatchResponse<LostMatchItem> matchLost(LostMatchRequest request) {
        long lostBoardId = parseLostBoardId(request.lostBoard());
        List<LostAcquiredRequest> candidates = request.acquiredBoardList();
        if (candidates == null || candidates.isEmpty()) {
            log.info("lost112 매칭: lostBoardId={}, 후보 0건 → 결과 없음", lostBoardId);
            return new MatchResponse<>(LOST_EMPTY, null);
        }
        MatchingSubject lost = lostSubject(request.lostBoard(), lostBoardId);

        List<Candidate<LostAcquiredRequest, Object>> parsed = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            LostAcquiredRequest c = candidates.get(i);
            if (c == null) {
                throw new BadRequestException("acquiredBoardList[" + i + "]는 null일 수 없습니다.");
            }
            String key = isBlank(c.atcId()) ? (c.id() == null ? "" : c.id()) : c.atcId();
            parsed.add(new Candidate<>(c, outputId(c.id()),
                    new MatchingSubject(key, c.mainPrdtClNm(), c.clrNm(), c.fdPrdtNm(), c.fdSbjt())));
        }

        List<LostMatchItem> result = rank(lost, parsed,
                (c, rate) -> new LostMatchItem(lostBoardId, c.parsedId(), rate, c.source().atcId(),
                        c.source().depPlace(), c.source().fdFilePathImg(), c.source().fdPrdtNm(), c.source().fdSbjt(),
                        c.source().clrNm(), c.source().fdYmd(), c.source().mainPrdtClNm()));
        log.info("lost112 매칭: lostBoardId={}, 후보 {}건 → 결과 {}건", lostBoardId, candidates.size(), result.size());
        return new MatchResponse<>(LOST_DONE, result);
    }

    /** 점수 계산 → [0,1] 자르기 → 5자리 반올림 → 내림차순(안정 정렬) → 상한. */
    private <S, I, R> List<R> rank(MatchingSubject lost, List<Candidate<S, I>> candidates,
                                   BiFunction<Candidate<S, I>, Double, R> toResult) {
        List<Scored<S, I>> scored = new ArrayList<>(candidates.size());
        for (Candidate<S, I> c : candidates) {
            scored.add(new Scored<>(c, normalize(scorer.score(lost, c.subject()))));
        }
        scored.sort(Comparator.comparingDouble((Scored<S, I> s) -> s.rate()).reversed());
        List<R> result = new ArrayList<>(Math.min(maxResults, scored.size()));
        for (int i = 0; i < scored.size() && i < maxResults; i++) {
            Scored<S, I> s = scored.get(i);
            result.add(toResult.apply(s.candidate(), s.rate()));
        }
        return result;
    }

    static double normalize(double raw) {
        if (Double.isNaN(raw)) {
            return 0.0;
        }
        double clamped = Math.max(0.0, Math.min(1.0, raw));
        return BigDecimal.valueOf(clamped).setScale(5, RoundingMode.HALF_UP).doubleValue();
    }

    private static MatchingSubject lostSubject(LostBoardRequest lost, long lostBoardId) {
        return new MatchingSubject(String.valueOf(lostBoardId), lost.categoryName(), lost.color(),
                lost.productName(), lost.description());
    }

    private static long parseLostBoardId(FindearMatchRequest request) {
        return parseLostBoardId(request.lostBoard());
    }

    private static long parseLostBoardId(LostBoardRequest lostBoard) {
        if (lostBoard == null) {
            throw new BadRequestException("lostBoard는 필수입니다.");
        }
        return parseLong(lostBoard.lostBoardId())
                .orElseThrow(() -> new BadRequestException("lostBoard.lostBoardId는 정수여야 합니다."));
    }

    /** id가 정수로 읽히면 Long, 아니면 원래 문자열, 없으면 null. */
    private static Object outputId(String id) {
        if (id == null) {
            return null;
        }
        return parseLong(id).<Object>map(v -> v).orElse(id);
    }

    private static Optional<Long> parseLong(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(value.strip()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private record Candidate<S, I>(S source, I parsedId, MatchingSubject subject) {
    }

    private record Scored<S, I>(Candidate<S, I> candidate, double rate) {
    }
}
