package com.findear.batch.police.controller;

import com.findear.batch.common.response.SuccessResponse;
import com.findear.batch.police.domain.PoliceAcquiredData;
import com.findear.batch.police.service.PoliceAcquiredDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 개발용 전체 조회·전체 삭제 (D-26: local 프로필에서만 등록된다. prod에서는 이 경로가 404 또는 405다) */
@Profile("local")
@RequiredArgsConstructor
@RequestMapping("/search")
@RestController
public class LocalPoliceAcquiredDataController {

    private final PoliceAcquiredDataService policeAcquiredDataService;

    /** Lost112 습득물 전체 */
    @GetMapping("/all")
    public ResponseEntity<?> searchAllDatas() {

        List<PoliceAcquiredData> result = policeAcquiredDataService.searchAllDatas();

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "모든 데이터 조회 성공", result));
    }

    /** Lost112 습득물 페이지 조회 (경로는 /save지만 이름과 달리 조회다. page는 0부터, 07 §3) */
    @GetMapping("/save")
    public ResponseEntity<?> searchByPage(@RequestParam("page") int page, @RequestParam("size") int size) {

        Page<PoliceAcquiredData> result = policeAcquiredDataService.searchByPage(page, size);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "모든 데이터 조회 성공", result));
    }

    /** Lost112 습득물 전체 삭제 */
    @DeleteMapping("")
    public ResponseEntity<?> deleteDatas() {

        policeAcquiredDataService.deleteDatas();

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "모든 데이터 삭제 성공", null));

    }
}
