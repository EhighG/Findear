package com.findear.batch.ours.controller;

import com.findear.batch.common.response.SuccessResponse;
import com.findear.batch.ours.dto.SearchPoliceBestMatchingListDto;
import com.findear.batch.ours.dto.SearchPoliceBoardMatchingListDto;
import com.findear.batch.ours.dto.SearchScrapBoardReqDto;
import com.findear.batch.ours.dto.SearchScrapBoardResDto;
import com.findear.batch.ours.service.PoliceDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lost112 매칭 목록·스크랩 조회 API (main이 호출). 전체 삭제는 {@link LocalPoliceDataController}(local 프로필 전용)에 있다.
 * 오류는 {@code CommonControllerAdvice}가 {@code {status, message}}로 바꾼다.
 */
@RequiredArgsConstructor
@RequestMapping("/police")
@RestController
public class PoliceDataController {

    private final PoliceDataService policeDataService;

    @GetMapping("/member/{memberId}")
    public ResponseEntity<?> searchPoliceBestMatchingList(@PathVariable Long memberId,
                                                    @RequestParam(required = false, defaultValue = "1") int page,
                                                    @RequestParam(required = false, defaultValue = "6") int size) {

        SearchPoliceBestMatchingListDto result = policeDataService.searchPoliceBestMatchingList(page, size, memberId);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "사용자의 findear 매칭 리스트 조회 성공", result));
    }

    @GetMapping("/board/{lostBoardId}")
    public ResponseEntity<?> searchPoliceBoardMatchingList(@PathVariable Long lostBoardId,
                                                     @RequestParam(required = false, defaultValue = "1") int page,
                                                     @RequestParam(required = false, defaultValue = "6") int size) {

        SearchPoliceBoardMatchingListDto result = policeDataService.searchPoliceBoardMatchingList(page, size, lostBoardId);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "사용자의 findear 매칭 리스트 조회 성공", result));
    }

    @PostMapping("/scrap")
    public ResponseEntity<?> searchScrapBoard(@RequestBody SearchScrapBoardReqDto searchScrapBoardReqDto) {

        List<SearchScrapBoardResDto> result = policeDataService.searchScrapBoard(searchScrapBoardReqDto);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "모든 데이터 조회 성공", result));

    }
}
