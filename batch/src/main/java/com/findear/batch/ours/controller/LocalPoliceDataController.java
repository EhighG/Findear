package com.findear.batch.ours.controller;

import com.findear.batch.common.response.SuccessResponse;
import com.findear.batch.ours.service.PoliceDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 개발용 전체 삭제 (D-26: local 프로필에서만 등록된다. prod에서는 이 경로가 404다) */
@Profile("local")
@RequiredArgsConstructor
@RequestMapping("/police")
@RestController
public class LocalPoliceDataController {

    private final PoliceDataService policeDataService;

    /** Lost112 매칭 로그 전체 삭제 */
    @DeleteMapping("")
    public ResponseEntity<?> deletePoliceMatchingDatas() {

        policeDataService.deletePoliceMatchingDatas();

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "모든 데이터 삭제 성공", null));

    }
}
