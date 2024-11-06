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

import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional
public class MatchingService {

    private final Lost112MatchingRepository lost112MatchingRepository;
    private final FindearMatchingRepository findearMatchingRepository;

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
