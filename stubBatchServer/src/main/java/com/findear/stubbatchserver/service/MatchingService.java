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

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
@Transactional
public class MatchingService {

    private final Lost112MatchingRepository lost112MatchingRepository;
    private final FindearMatchingRepository findearMatchingRepository;

    public FindearMatchingListResDto getFindearMatchingList(Long lostBoardId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<FindearMatching> matchingListPage = findearMatchingRepository
                .findAllByLostBoardIdOrderBySimilarityRateDesc(pageable, lostBoardId);
        // Entity to DTO
        List<FindearMatchingDto> matchingList = matchingListPage.getContent()
                .stream().map(FindearMatchingDto::of)
                .toList();
        return new FindearMatchingListResDto(matchingList, matchingList.size());
    }

    public Lost112MatchingListResDto getLost112MatchingList(Long lostBoardId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Lost112Matching> matchingListPage = lost112MatchingRepository
                .findAllByLostBoardIdOrderBySimilarityRateDesc(pageable, lostBoardId);
        // Entity to DTO
        List<Lost112MatchingDto> matchingList = new ArrayList<>(matchingListPage.getNumberOfElements());
        matchingListPage
                .forEach(matching -> {
                    matchingList.add(Lost112MatchingDto.of(matching));
                });
        return new Lost112MatchingListResDto(matchingList, matchingList.size());
    }
}
