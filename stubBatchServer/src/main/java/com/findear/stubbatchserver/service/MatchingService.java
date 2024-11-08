package com.findear.stubbatchserver.service;

import com.findear.stubbatchserver.domain.FindearMatching;
import com.findear.stubbatchserver.domain.Lost112Matching;
import com.findear.stubbatchserver.dto.FindearMatchingListResDto;
import com.findear.stubbatchserver.dto.FindearMatchingDto;
import com.findear.stubbatchserver.dto.Lost112MatchingDto;
import com.findear.stubbatchserver.dto.Lost112MatchingListResDto;
import com.findear.stubbatchserver.repository.FindearMatchingRepository;
import com.findear.stubbatchserver.repository.Lost112MatchingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

@RequiredArgsConstructor
@Service
@Transactional
public class MatchingService {
    private final FindearMatchingRepository findearMatchingRepository;

    private final Lost112MatchingRepository lost112MatchingRepository;

    public FindearMatchingListResDto getFindearBestMatching(Long memberId, int page, int size) {
        List<FindearMatching> matchingList = findearMatchingRepository.findAllByLostMemberId(memberId);
        List<FindearMatching> bestMatchings = extractFindearBests(matchingList);

        return getFindearPageResponse(bestMatchings, page, size);
    }

    public FindearMatchingListResDto getFindearMatchingList(Long lostBoardId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<FindearMatching> matchingPage = findearMatchingRepository
                .findAllByLostBoardIdOrderBySimilarityRateDesc(pageable, lostBoardId);

        return findearPageToResponse(matchingPage);
    }

    public Lost112MatchingListResDto getLost112BestMatching(Long memberId, int page, int size) {
        List<Lost112Matching> matchingList = lost112MatchingRepository.findAllByLostMemberId(memberId);
        List<Lost112Matching> bestMatchings = extractLost112Bests(matchingList);

        return getLost112PageResponse(bestMatchings, page, size);
    }

    public Lost112MatchingListResDto getLost112MatchingList(Long lostBoardId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Lost112Matching> matchingPage = lost112MatchingRepository
                .findAllByLostBoardIdOrderBySimilarityRateDesc(pageable, lostBoardId);

        return lost112PageToResponse(matchingPage);
    }

    private List<FindearMatching> extractFindearBests(List<FindearMatching> matchingList) {
        Map<Long, FindearMatching> bests = new HashMap<>();

        for (FindearMatching matching : matchingList) {
            Long key = matching.getLostBoardId();
            bests.put(key, bests.containsKey(key) ?
                    getMoreSimilar(matching, bests.get(key)) // 기존 값이 있다면, 유사도가 높은 것을 남김
                    : matching); // 없다면, 현재 값 추가
        }

        List<FindearMatching> bestMatchings = new ArrayList<>(bests.values());
        // 유사도 순 정렬
        bestMatchings.sort((m1, m2) -> m1.getSimilarityRate() > m2.getSimilarityRate() ? -1 : 1);
        return bestMatchings;
    }

    private FindearMatchingListResDto getFindearPageResponse(List<FindearMatching> bestMatchings, int page, int size) {
        List<FindearMatching> slicedData = slicePage(bestMatchings, page, size);
        if (slicedData.isEmpty()) {
            return new FindearMatchingListResDto(new ArrayList<>(), 0);
        }

        List<FindearMatchingDto> pagingResult = slicedData.stream()
                .map(FindearMatchingDto::of)
                .toList();
        return new FindearMatchingListResDto(pagingResult, bestMatchings.size());
    }

    private FindearMatchingListResDto findearPageToResponse(Page<FindearMatching> matchingPage) {
        List<FindearMatchingDto> dtoList = matchingPage.getContent().stream()
                .map(FindearMatchingDto::of)
                .toList();
        return new FindearMatchingListResDto(dtoList, matchingPage.getTotalElements());
    }

    private FindearMatching getMoreSimilar(FindearMatching matching1, FindearMatching matching2) {
        return matching1.getSimilarityRate() > matching2.getSimilarityRate()
                ? matching1
                : matching2;
    }

    private List<Lost112Matching> extractLost112Bests(List<Lost112Matching> matchingList) {
        Map<Long, Lost112Matching> bests = new HashMap<>();

        for (Lost112Matching matching : matchingList) {
            Long key = matching.getLostBoardId();
            bests.put(key, bests.containsKey(key) ?
                    getMoreSimilar(matching, bests.get(key)) // 기존 값이 있다면, 유사도가 높은 것을 남김
                    : matching); // 없다면, 현재 값 추가
        }

        List<Lost112Matching> bestMatchings = new ArrayList<>(bests.values());
        // 유사도 순 정렬
        bestMatchings.sort((m1, m2) -> m1.getSimilarityRate() > m2.getSimilarityRate() ? -1 : 1);
        return bestMatchings;
    }

    private Lost112MatchingListResDto getLost112PageResponse(List<Lost112Matching> bestMatchings, int page, int size) {
        List<Lost112Matching> slicedData = slicePage(bestMatchings, page, size);
        if (slicedData.isEmpty()) {
            return new Lost112MatchingListResDto(new ArrayList<>(), 0);
        }

        List<Lost112MatchingDto> pagingResult = slicedData.stream()
                .map(Lost112MatchingDto::of)
                .toList();
        return new Lost112MatchingListResDto(pagingResult, bestMatchings.size());
    }

    private Lost112MatchingListResDto lost112PageToResponse(Page<Lost112Matching> matchingPage) {
        List<Lost112MatchingDto> dtoList = matchingPage.getContent().stream()
                .map(Lost112MatchingDto::of)
                .toList();
        return new Lost112MatchingListResDto(dtoList, matchingPage.getTotalElements());
    }

    private Lost112Matching getMoreSimilar(Lost112Matching matching1, Lost112Matching matching2) {
        return matching1.getSimilarityRate() > matching2.getSimilarityRate()
                ? matching1
                : matching2;
    }

    private <T> List<T> slicePage(List<T> totalList, int page, int size) {
        int start = (page - 1) * size;
        int end = Math.min(start + size, totalList.size());

        return start < end ? totalList.subList(start, end) : new ArrayList<>();
    }
}
