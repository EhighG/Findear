package com.findear.batch.ours.controller;

import com.findear.batch.common.response.SuccessResponse;
import com.findear.batch.ours.dto.*;
import com.findear.batch.ours.service.FindearDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Findear 매칭 API (main이 호출). 전체 조회·전체 삭제는 {@link LocalFindearDataController}(local 프로필 전용)에 있다.
 * 오류는 {@code CommonControllerAdvice}가 {@code {status, message}}로 바꾼다.
 */
@RequiredArgsConstructor
@RequestMapping("/findear")
@RestController
public class FindearDataController {

    private final FindearDataService findearDataService;

    @PostMapping(value = "/matching")
    public ResponseEntity<?> matchingFindearDatas(@RequestBody LostBoardMatchingDto lostBoardMatchingDto) {

        MatchingAllDatasToAiResDto result = findearDataService.matchingFindearDatas(lostBoardMatchingDto);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "습득물 매칭 성공", result));

    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<?> searchBestMatchingList(@PathVariable Long memberId,
                                                       @RequestParam(required = false, defaultValue = "1") int page,
                                                       @RequestParam(required = false, defaultValue = "6") int size) {

        SearchFindearBestMatchingListDto result = findearDataService.searchBestMatchingList(page, size, memberId);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "사용자의 findear 매칭 리스트 조회 성공", result));
    }

    @GetMapping("/board/{lostBoardId}")
    public ResponseEntity<?> searchBoardMatchingList(@PathVariable Long lostBoardId,
                                                       @RequestParam(required = false, defaultValue = "1") int page,
                                                       @RequestParam(required = false, defaultValue = "6") int size) {

        SearchFindearBoardMatchingListDto result = findearDataService.searchBoardMatchingList(page, size, lostBoardId);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "사용자의 findear 매칭 리스트 조회 성공", result));
    }
}
