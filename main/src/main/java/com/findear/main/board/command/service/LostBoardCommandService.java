package com.findear.main.board.command.service;

import com.findear.main.board.command.dto.ModifyLostBoardReqDto;
import com.findear.main.board.command.dto.PostLostBoardReqDto;

public interface LostBoardCommandService {
    Long register(PostLostBoardReqDto postLostBoardReqDto);
    Long modify(ModifyLostBoardReqDto modifyReqDto);
    void remove(Long boardId, Long memberId);
}
