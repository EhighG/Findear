package com.findear.batch.ours.controller;

import com.findear.batch.common.response.SuccessResponse;
import com.findear.batch.ours.domain.PoliceMatchingLog;
import com.findear.batch.ours.dto.SearchPoliceBestMatchingListDto;
import com.findear.batch.ours.dto.SearchPoliceBoardMatchingListDto;
import com.findear.batch.ours.dto.SearchScrapBoardReqDto;
import com.findear.batch.ours.dto.SearchScrapBoardResDto;
import com.findear.batch.ours.service.PoliceDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/police")
@RestController
public class PoliceDataController {

    private final PoliceDataService policeDataService;

    @GetMapping("/member/{memberId}")
    public ResponseEntity<?> searchPoliceBestMatchingList(@PathVariable Long memberId,
                                                    @RequestParam(required = false, defaultValue = "1") int page,
                                                    @RequestParam(required = false, defaultValue = "6") int size) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        SearchPoliceBestMatchingListDto result = policeDataService.searchPoliceBestMatchingList(page, size, memberId);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "사용자의 findear 매칭 리스트 조회 성공", result));
    }

    @GetMapping("/board/{lostBoardId}")
    public ResponseEntity<?> searchPoliceBoardMatchingList(@PathVariable Long lostBoardId,
                                                     @RequestParam(required = false, defaultValue = "1") int page,
                                                     @RequestParam(required = false, defaultValue = "6") int size) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        SearchPoliceBoardMatchingListDto result = policeDataService.searchPoliceBoardMatchingList(page, size, lostBoardId);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "사용자의 findear 매칭 리스트 조회 성공", result));
    }

    @PostMapping("/scrap")
    public ResponseEntity<?> searchScrapBoard(@RequestBody SearchScrapBoardReqDto searchScrapBoardReqDto) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        List<SearchScrapBoardResDto> result = policeDataService.searchScrapBoard(searchScrapBoardReqDto);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "모든 데이터 조회 성공", result));

    }

    @GetMapping("/test-api")
    public ResponseEntity<?> testApi() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        Page<PoliceMatchingLog> result = policeDataService.testApi();

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "모든 데이터 조회 성공", result));

    }

    @DeleteMapping
    public ResponseEntity<?> deletePoliceMatchingDatas() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        policeDataService.deletePoliceMatchingDatas();

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "모든 데이터 삭제 성공", null));

    }
}

