package com.findear.main.board.command.controller;

import com.findear.main.board.command.dto.ModifyLostBoardReqDto;
import com.findear.main.board.command.dto.PostLostBoardReqDto;
import com.findear.main.board.command.service.LostBoardCommandService;
import com.findear.main.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/losts")
@RestController
public class LostBoardCommandController {

    private final LostBoardCommandService lostBoardCommandService;

    @PostMapping
    public ResponseEntity<?> register(@AuthenticationPrincipal Long memberId,
            @RequestBody PostLostBoardReqDto postLostBoardReqDto) {
        postLostBoardReqDto.setMemberId(memberId);

        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "등록되었습니다.",
                        lostBoardCommandService.register(postLostBoardReqDto)));
    }

    @PatchMapping("/{boardId}")
    public ResponseEntity<?> modify(@AuthenticationPrincipal Long memberId,
                                    @PathVariable Long boardId,
                                    @RequestBody ModifyLostBoardReqDto modifyLostBoardReqDto) {
        modifyLostBoardReqDto.setMemberId(memberId);
        modifyLostBoardReqDto.setBoardId(boardId);
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(),
                        "변경되었습니다.",
                        lostBoardCommandService.modify(modifyLostBoardReqDto)));
    }

    @PatchMapping("/{boardId}/delete")
    public ResponseEntity<?> remove(@AuthenticationPrincipal Long memberId,
                                    @PathVariable Long boardId) {
        lostBoardCommandService.remove(boardId, memberId);
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(),
                        "삭제되었습니다."));
    }
}
