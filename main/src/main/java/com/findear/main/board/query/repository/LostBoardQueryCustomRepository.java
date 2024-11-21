package com.findear.main.board.query.repository;

import com.findear.main.board.query.dto.FindAllLostBoardReqDto;
import com.findear.main.board.query.dto.LostBoardListResDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LostBoardQueryCustomRepository {
    Page<LostBoardListResDto> findAllWithDtoForm(FindAllLostBoardReqDto findAllReq, Pageable pageable);
}
