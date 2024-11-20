package com.findear.main.board.query.service;

import com.findear.main.board.query.dto.FindAllLostBoardReqDto;
import com.findear.main.board.query.dto.LostBoardDetailResDto;
import com.findear.main.board.query.dto.LostBoardListResponse;

public interface LostBoardQueryService {
    // TODO: 매개변수 DTO로 변경
    LostBoardListResponse findAll(FindAllLostBoardReqDto findAllReq);
    LostBoardDetailResDto findById(Long lostBoardId);
    LostBoardDetailResDto findByBoardId(Long boardId);
}
