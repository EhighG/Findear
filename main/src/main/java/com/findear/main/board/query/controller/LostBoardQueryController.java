package com.findear.main.board.query.controller;

import com.findear.main.board.common.domain.BoardDto;
import com.findear.main.board.common.domain.LostBoardDto;
import com.findear.main.board.common.domain.ImgFile;
import com.findear.main.board.query.dto.LostBoardListResDto;
import com.findear.main.board.query.service.LostBoardQueryService;
import com.findear.main.common.response.SuccessResponse;
import com.findear.main.member.command.dto.BriefMemberDto;
import com.findear.main.member.common.domain.Role;
import com.findear.main.member.common.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/losts")
@RestController
public class LostBoardQueryController {

    private final LostBoardQueryService lostBoardQueryService;

    @GetMapping
    public ResponseEntity<?> findLosts(@RequestParam(required = false) String category,
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
                        lostBoardQueryService.findAll(memberId, category, sDate, eDate, keyword, sortBy, desc, pageNo, size)));
    }

    @GetMapping("/{boardId}")
    public ResponseEntity<?> findById(@PathVariable Long boardId) {
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "조회에 성공했습니다.", lostBoardQueryService.findByBoardId(boardId)));
    }
}
