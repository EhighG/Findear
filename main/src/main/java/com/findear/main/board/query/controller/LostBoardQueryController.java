package com.findear.main.board.query.controller;

import com.findear.main.board.query.dto.FindAllLostBoardReqDto;
import com.findear.main.board.query.service.LostBoardQueryService;
import com.findear.main.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/losts")
@RestController
public class LostBoardQueryController {

    private final LostBoardQueryService lostBoardQueryService;

    @GetMapping
    public ResponseEntity<?> findAll(@ModelAttribute FindAllLostBoardReqDto findAllReqDto) {
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "조회에 성공했습니다.",
                        lostBoardQueryService.findAll(findAllReqDto)));
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<?> findByBoardId(@PathVariable Long boardId) {
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "조회에 성공했습니다.", lostBoardQueryService.findByBoardId(boardId)));
    }
}
