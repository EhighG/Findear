package com.findear.main.matching.service;

import com.findear.main.matching.model.dto.FindearMatchingListResDto;
import com.findear.main.matching.model.dto.Lost112MatchingListResDto;

public interface MatchingService {
    FindearMatchingListResDto getFindearBestMatchings(Long memberId, int pageNo, int size);
    FindearMatchingListResDto getFindearMatchingList(Long lostBoardId, int pageNo, int size);
    Lost112MatchingListResDto getLost112BestMatchings(Long memberId, int pageNo, int size);
    Lost112MatchingListResDto getLost112MatchingList(Long lostBoardId, int pageNo, int size);
}
