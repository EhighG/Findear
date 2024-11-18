package com.findear.main.board.command.controller;

import com.findear.main.board.command.dto.GiveBackReqDto;
import com.findear.main.board.command.dto.ModifyAcquiredBoardReqDto;
import com.findear.main.board.command.dto.PostAcquiredBoardReqDto;
import com.findear.main.board.command.service.AcquiredBoardCommandService;
import com.findear.main.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/acquisitions")
@RequiredArgsConstructor
@RestController
public class AcquiredBoardCommandController {

    private final AcquiredBoardCommandService acquiredBoardCommandService;

    @PostMapping
    public ResponseEntity<?> register(@AuthenticationPrincipal Long memberId,
                                      @RequestBody PostAcquiredBoardReqDto postAcquiredBoardReqDto) {
        postAcquiredBoardReqDto.setMemberId(memberId);
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "등록되었습니다.",
                        acquiredBoardCommandService.register(postAcquiredBoardReqDto)));
    }

    @PatchMapping("/{boardId}")
    public ResponseEntity<?> modify(@AuthenticationPrincipal Long memberId,
                                    @PathVariable Long boardId,
                                    @RequestBody ModifyAcquiredBoardReqDto modifyReqDto) {
        modifyReqDto.setMemberId(memberId);
        modifyReqDto.setBoardId(boardId);
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(),
                        "변경되었습니다.",
                        acquiredBoardCommandService.modify(modifyReqDto)));
    }

    @PatchMapping("{boardId}/delete")
    public ResponseEntity<?> remove(@AuthenticationPrincipal Long memberId,
                                    @PathVariable Long boardId) {
        acquiredBoardCommandService.remove(boardId, memberId);
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(),
                        "삭제되었습니다."));
    }

    @PostMapping("/{boardId}/return")
    public ResponseEntity<?> giveBack(@AuthenticationPrincipal Long managerId,
                                      @PathVariable Long boardId,
                                      @RequestBody GiveBackReqDto giveBackReqDto) {
        giveBackReqDto.setManagerId(managerId);
        giveBackReqDto.setBoardId(boardId);
        acquiredBoardCommandService.giveBack(giveBackReqDto);

        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "인계처리 되었습니다."));
    }

    @PatchMapping("/{boardId}/rollback")
    public ResponseEntity<?> cancelGiveBack(@AuthenticationPrincipal Long managerId,
                                            @PathVariable Long boardId) {
        acquiredBoardCommandService.cancelGiveBack(managerId, boardId);
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "인계가 취소되었습니다."));
    }

    // 스크랩
    @PostMapping("/{boardId}/scrap")
    public ResponseEntity<?> scrap(@AuthenticationPrincipal Long memberId,
                                   @PathVariable String boardId,
                                   @RequestParam Boolean isFindear) {
        acquiredBoardCommandService.scrap(memberId, boardId, isFindear);
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "스크랩 되었습니다."));
    }

    @DeleteMapping("/{boardId}/scrap")
    public ResponseEntity<?> cancelScrap(@AuthenticationPrincipal Long memberId,
                                         @PathVariable String boardId,
                                         @RequestParam Boolean isFindear) {
        acquiredBoardCommandService.cancelScrap(memberId, boardId, isFindear);
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "스크랩 취소되었습니다."));
    }

    @GetMapping("/scraps")
    public ResponseEntity<?> findScrapList(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "조회에 성공했습니다.",
                        acquiredBoardCommandService.findScrapList(memberId)));
    }
}

