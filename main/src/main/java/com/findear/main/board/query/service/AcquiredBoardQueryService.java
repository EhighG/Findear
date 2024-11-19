package com.findear.main.board.query.service;

import com.findear.main.board.query.dto.AcquiredBoardDetailResDto;
import com.findear.main.board.query.dto.AcquiredBoardListResponse;

import java.util.List;
import java.util.Map;

public interface AcquiredBoardQueryService {
    AcquiredBoardListResponse findAll(Long memberId, String category, String sDate, String eDate, String keyword,
                                      String sortBy, Boolean desc, int pageNo, int pageSize);
    List<?> findAllInLost112(String category, String sDate, String eDate, String keyword, int pageNo,
                             int pageSize);
    AcquiredBoardDetailResDto findById(Long acquiredBoardId);
    AcquiredBoardDetailResDto findByBoardId(Long boardId);
    Integer getLost112TotalPageNum(int pageSize);
    Map<String, Long> getYesterdaysReturnCount();
}
