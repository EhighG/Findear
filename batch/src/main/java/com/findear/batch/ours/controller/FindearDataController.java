package com.findear.batch.ours.controller;

import com.findear.batch.common.response.SuccessResponse;
import com.findear.batch.ours.domain.FindearMatchingLog;
import com.findear.batch.ours.dto.*;
import com.findear.batch.ours.service.FindearDataService;
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
@RequestMapping("/findear")
@RestController
public class FindearDataController {

    private final FindearDataService findearDataService;

    @PostMapping(value = "/matching")
    public ResponseEntity<?> matchingFindearDatas(@RequestBody LostBoardMatchingDto lostBoardMatchingDto) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        log.info("분실물 매칭");
        MatchingAllDatasToAiResDto result = findearDataService.matchingFindearDatas(lostBoardMatchingDto);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "습득물 매칭 성공", result));

    }

    @PostMapping("/matching/batch")
    public ResponseEntity<?> matchingFindearDatasBatch() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        List<MatchingFindearDatasToAiResDto> result = findearDataService.matchingFindearDatasBatch();

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "batch용 매칭 테스트 성공", result));

    }

    @GetMapping("")
    public ResponseEntity<?> searchAllFindearMatchingList() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        List<SearchFindearMatchingListResDto> result = findearDataService.searchAllFindearMatchingList();

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "findear 매칭 리스트 조회 성공", result));
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<?> searchBestMatchingList(@PathVariable Long memberId,
                                                       @RequestParam(required = false, defaultValue = "1") int page,
                                                       @RequestParam(required = false, defaultValue = "6") int size) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        SearchFindearBestMatchingListDto result = findearDataService.searchBestMatchingList(page, size, memberId);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "사용자의 findear 매칭 리스트 조회 성공", result));
    }

    @GetMapping("/board/{lostBoardId}")
    public ResponseEntity<?> searchBoardMatchingList(@PathVariable Long lostBoardId,
                                                       @RequestParam(required = false, defaultValue = "1") int page,
                                                       @RequestParam(required = false, defaultValue = "6") int size) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        SearchFindearBoardMatchingListDto result = findearDataService.searchBoardMatchingList(page, size, lostBoardId);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "사용자의 findear 매칭 리스트 조회 성공", result));
    }

    @GetMapping("/test-api")
    public ResponseEntity<?> testApi() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        Page<FindearMatchingLog> result = findearDataService.testApi();

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "모든 데이터 조회 성공", result));

    }

    @DeleteMapping
    public ResponseEntity<?> deleteFindearMatchingDatas() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        findearDataService.deleteFindearMatchingDatas();

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "모든 데이터 삭제 성공", null));

    }
}