package com.findear.main.board.command.service;

import com.findear.main.board.command.dto.GiveBackReqDto;
import com.findear.main.board.command.dto.ModifyAcquiredBoardReqDto;
import com.findear.main.board.command.dto.PostAcquiredBoardReqDto;
import com.findear.main.board.common.dto.ScrapListResDto;

public interface AcquiredBoardCommandService {
    Long register(PostAcquiredBoardReqDto postAcquiredBoardReqDto);
    Long modify(ModifyAcquiredBoardReqDto modifyReqDto);
    void remove(Long boardId, Long memberId);
    void giveBack(GiveBackReqDto giveBackReqDto);
    void cancelGiveBack(Long managerId, Long boardId);
    void scrap(Long memberId, String boardId, Boolean isFindear);
    // FIXME: 해당 기능 query 패키지로 옮기기
    ScrapListResDto findScrapList(Long memberId);
    void cancelScrap(Long memberId, String boardId, Boolean isFindear);
}
