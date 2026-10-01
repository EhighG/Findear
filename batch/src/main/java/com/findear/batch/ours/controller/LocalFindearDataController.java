package com.findear.batch.ours.controller;

import com.findear.batch.common.response.SuccessResponse;
import com.findear.batch.ours.dto.SearchFindearMatchingListResDto;
import com.findear.batch.ours.service.FindearDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 개발용 전체 조회·전체 삭제 (D-26: local 프로필에서만 등록된다. prod에서는 이 경로가 404다) */
@Profile("local")
@RequiredArgsConstructor
@RequestMapping("/findear")
@RestController
public class LocalFindearDataController {

    private final FindearDataService findearDataService;

    /** Findear 매칭 로그 전체 */
    @GetMapping("")
    public ResponseEntity<?> searchAllFindearMatchingList() {

        List<SearchFindearMatchingListResDto> result = findearDataService.searchAllFindearMatchingList();

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "findear 매칭 리스트 조회 성공", result));
    }

    /** Findear 매칭 로그 전체 삭제 */
    @DeleteMapping("")
    public ResponseEntity<?> deleteFindearMatchingDatas() {

        findearDataService.deleteFindearMatchingDatas();

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "모든 데이터 삭제 성공", null));

    }
}
