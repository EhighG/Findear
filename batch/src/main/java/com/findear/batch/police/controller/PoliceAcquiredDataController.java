package com.findear.batch.police.controller;

import com.findear.batch.common.response.SuccessResponse;
import com.findear.batch.police.domain.PoliceAcquiredData;
import com.findear.batch.police.service.Lost112CollectResult;
import com.findear.batch.police.service.Lost112CollectService;
import com.findear.batch.police.service.PoliceAcquiredDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lost112 습득물 조회·수집 API. 전체 조회·페이지 조회·전체 삭제는 {@link LocalPoliceAcquiredDataController}(local 프로필 전용)에 있다.
 * 오류는 {@code CommonControllerAdvice}(와 {@code Lost112ExceptionAdvice})가 {@code {status, message}}로 바꾼다.
 */
@RequiredArgsConstructor
@RequestMapping("/search")
@RestController
public class PoliceAcquiredDataController {

    private final PoliceAcquiredDataService policeAcquiredDataService;
    private final Lost112CollectService lost112CollectService;

    @GetMapping("")
    public ResponseEntity<?> search(@RequestParam("page") int page, @RequestParam("size") int size,
                                    @RequestParam(value = "category", required = false) String category,
                                    @RequestParam(value = "startDate", required = false) String startDate,
                                    @RequestParam(value = "endDate", required = false) String endDate,
                                    @RequestParam(value = "keyword", required = false) String keyword) {

        List<PoliceAcquiredData> result = policeAcquiredDataService.search(page, size, category, startDate, endDate, keyword);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "모든 데이터 조회 성공", result));
    }

    @GetMapping("/total")
    public ResponseEntity<?> totalCount() {

        long result = policeAcquiredDataService.getTotalCount();

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "모든 데이터 갯수", result));
    }

    @PostMapping("/save")
    public ResponseEntity<?> savePoliceData() {

        // 키가 없으면 503, 모든 서비스가 실패해 하나도 못 넣었으면 502 (Lost112ExceptionAdvice). 일부만 실패하면 200 + 요약의 error
        Lost112CollectResult result = lost112CollectService.collectOrThrow();

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "로스트112 데이터 저장 성공", result));
    }
}
