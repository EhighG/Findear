package com.findear.main.board.query.controller;

import com.findear.main.board.common.domain.AcquiredBoardDto;
import com.findear.main.board.query.dto.AcquiredBoardListResDto;
import com.findear.main.board.query.service.AcquiredBoardQueryService;
import com.findear.main.common.response.SuccessResponse;
import com.findear.main.member.command.dto.BriefMemberDto;
import com.findear.main.member.command.dto.LoginResAgencyDto;
import com.findear.main.member.common.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/acquisitions")
@RestController
public class AcquiredBoardQueryController {

    private final AcquiredBoardQueryService acquiredBoardQueryService;

    @GetMapping
    public ResponseEntity<?> findAll(@RequestParam(required=false) String category,
                                                @RequestParam(required = false) Long memberId,
                                                @RequestParam(required = false) String sDate,
                                                @RequestParam(required = false) String eDate,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String sortBy,
                                                @RequestParam(required = false, defaultValue = "true") Boolean desc,
                                                @RequestParam(required = false, defaultValue = "1") Integer pageNo,
                                                @RequestParam(required = false, defaultValue = "10") Integer size) {
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "조회에 성공했습니다.",
                        acquiredBoardQueryService.findAll(memberId, category, sDate, eDate, keyword, sortBy, desc, pageNo, size)));
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<?> findByBoardId(@PathVariable Long boardId) {
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "조회에 성공했습니다.", acquiredBoardQueryService.findByBoardId(boardId)));
    }

    @GetMapping("/lost112")
    public ResponseEntity<?> findAllInLost112(@RequestParam(required=false) String category,
                                              @RequestParam(required = false) String sDate,
                                              @RequestParam(required = false) String eDate,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false, defaultValue = "1") Integer pageNo,
                                              @RequestParam(required = false, defaultValue = "10") Integer size) {
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "조회에 성공했습니다.",
                        acquiredBoardQueryService.findAllInLost112(category, sDate, eDate, keyword, pageNo, size)));
    }

    @GetMapping("/lost112/total-page")
    public ResponseEntity<?> getLost112TotalPageNum(@RequestParam Integer size) {
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "조회에 성공했습니다.",
                        acquiredBoardQueryService.getLost112TotalPageNum(size)));
    }

    @GetMapping("/returns/count")
    public ResponseEntity<?> getYesterdaysReturnCount() {
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "조회에 성공했습니다.",
                        acquiredBoardQueryService.getYesterdaysReturnCount()));
    }
}
