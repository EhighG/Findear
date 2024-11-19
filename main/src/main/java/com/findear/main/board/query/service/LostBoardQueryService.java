package com.findear.main.board.query.service;

import com.findear.main.board.query.dto.LostBoardDetailResDto;
import com.findear.main.board.query.dto.LostBoardListResponse;

public interface LostBoardQueryService {
    LostBoardListResponse findAll(Long memberId, String category, String sDate, String eDate, String keyword,
                                  String sortBy, Boolean desc, int pageNo, int size);
    LostBoardDetailResDto findByBoardId(Long boardId);
    LostBoardDetailResDto findById(Long lostBoardId);
}
