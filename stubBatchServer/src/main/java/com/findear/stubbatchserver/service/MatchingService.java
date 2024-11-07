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
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional
public class MatchingService {

    private final Lost112MatchingRepository lost112MatchingRepository;
    private final FindearMatchingRepository findearMatchingRepository;

    public FindearMatchingListResDto getFindearBestMatching(Long memberId, int page, int size) {
        List<FindearMatching> matchingList = findearMatchingRepository.findAllByLostMemberId(memberId);
        List<FindearMatching> bestMatchings = extractBestMatchings(matchingList);

        return getPageResponse(bestMatchings, page, size);
    }

    private FindearMatchingListResDto getPageResponse(List<FindearMatching> bestMatchings, int page, int size) {
        int start = (page - 1) * size;
        int end = Math.min(start + size, bestMatchings.size());

        if (start >= end) {
            return new FindearMatchingListResDto(new ArrayList<>(), bestMatchings.size());
        }

        List<FindearMatchingDto> pagingResult = bestMatchings.subList(start, end).stream()
                .map(FindearMatchingDto::of)
                .toList();
        return new FindearMatchingListResDto(pagingResult, bestMatchings.size());
    }

    private List<FindearMatching> extractBestMatchings(List<FindearMatching> matchingList) {
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

    private FindearMatching getMoreSimilar(FindearMatching matching1, FindearMatching matching2) {
        return matching1.getSimilarityRate() > matching2.getSimilarityRate()
                ? matching1
                : matching2;
    }

    public FindearMatchingListResDto getFindearMatchingList(Long lostBoardId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<FindearMatching> matchingPage = findearMatchingRepository
                .findAllByLostBoardIdOrderBySimilarityRateDesc(pageable, lostBoardId);

        return findearPageToResponse(matchingPage);
    }

    public Lost112MatchingListResDto getLost112MatchingList(Long lostBoardId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Lost112Matching> matchingPage = lost112MatchingRepository
                .findAllByLostBoardIdOrderBySimilarityRateDesc(pageable, lostBoardId);

        return lost112PageToResponse(matchingPage);
    }

    private FindearMatchingListResDto findearPageToResponse(Page<FindearMatching> matchingPage) {
        List<FindearMatchingDto> dtoList = matchingPage.getContent().stream()
                .map(FindearMatchingDto::of)
                .toList();
        return new FindearMatchingListResDto(dtoList, matchingPage.getTotalElements());
    }

    private Lost112MatchingListResDto lost112PageToResponse(Page<Lost112Matching> matchingPage) {
        List<Lost112MatchingDto> dtoList = matchingPage.getContent().stream()
                .map(Lost112MatchingDto::of)
                .toList();
        return new Lost112MatchingListResDto(dtoList, matchingPage.getTotalElements());
    }
}
