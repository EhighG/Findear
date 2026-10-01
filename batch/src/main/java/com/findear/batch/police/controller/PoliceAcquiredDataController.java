package com.findear.batch.police.controller;

import com.findear.batch.common.response.SuccessResponse;
import com.findear.batch.ours.domain.FindearMatchingLog;
import com.findear.batch.police.domain.PoliceAcquiredData;
import com.findear.batch.police.dto.SaveDataRequestDto;
import com.findear.batch.police.service.Lost112CollectResult;
import com.findear.batch.police.service.Lost112CollectService;
import com.findear.batch.police.service.PoliceAcquiredDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/search")
@RestController
public class PoliceAcquiredDataController {

    private final PoliceAcquiredDataService policeAcquiredDataService;
    private final Lost112CollectService lost112CollectService;

    @GetMapping("/all")
    public ResponseEntity<?> searchAllDatas() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        List<PoliceAcquiredData> result = policeAcquiredDataService.searchAllDatas();

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "모든 데이터 조회 성공", result));
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "ㅎㅇ", null));
    }


    @GetMapping("")
    public ResponseEntity<?> search(@RequestParam("page") int page, @RequestParam("size") int size,
                                    @RequestParam(value = "category", required = false) String category,
                                    @RequestParam(value = "startDate", required = false) String startDate,
                                    @RequestParam(value = "endDate", required = false) String endDate,
                                    @RequestParam(value = "keyword", required = false) String keyword) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        List<PoliceAcquiredData> result = policeAcquiredDataService.search(page, size, category, startDate, endDate, keyword);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "모든 데이터 조회 성공", result));
    }

    @GetMapping("/total")
    public ResponseEntity<?> totalCount() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        long result = policeAcquiredDataService.getTotalCount();

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "모든 데이터 갯수", result));
    }

    @GetMapping("/save")
    public ResponseEntity<?> searchByPage(@RequestParam("page") int page, @RequestParam("size") int size) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        Page<PoliceAcquiredData> result = policeAcquiredDataService.searchByPage(page, size);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "모든 데이터 조회 성공", result));
    }

    @PostMapping("/save")
    public ResponseEntity<?> savePoliceData() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        // 키가 없으면 503, 모든 서비스가 실패해 하나도 못 넣었으면 502 (Lost112ExceptionAdvice). 일부만 실패하면 200 + 요약의 error
        Lost112CollectResult result = lost112CollectService.collectOrThrow();

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "로스트112 데이터 저장 성공", result));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteDatas() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        policeAcquiredDataService.deleteDatas();

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "모든 데이터 삭제 성공", null));

    }

}
