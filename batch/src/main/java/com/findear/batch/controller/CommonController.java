package com.findear.batch.controller;


import com.findear.batch.Lost112BoardListReqDto;
import com.findear.batch.common.SuccessResponse;
import com.findear.batch.dto.AutoFillReqDto;
import com.findear.batch.dto.FindearMatchingListResDto;
import com.findear.batch.dto.Lost112AcquiredDto;
import com.findear.batch.dto.Lost112MatchingListResDto;
import com.findear.batch.service.Lost112AcquiredService;
import com.findear.batch.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/")
public class CommonController {

    private final Lost112AcquiredService lost112AcquiredService;
    private final MatchingService matchingService;

    @GetMapping("/findear/member/{memberId}")
    public ResponseEntity<?> getFindearBestMatching(@PathVariable Long memberId,
                                                    @RequestParam(required = false, defaultValue = "1") int page,
                                                    @RequestParam(required = false, defaultValue = "6") int size) {
        FindearMatchingListResDto bestMatchings = matchingService.getFindearBestMatching(memberId, page, size);

        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "success", bestMatchings));
    }

    @GetMapping("/findear/board/{lostBoardId}")
    public ResponseEntity<?> getFindearMatchingList(@PathVariable Long lostBoardId,
                                                    @RequestParam(required = false, defaultValue = "1") int page,
                                                    @RequestParam(required = false, defaultValue = "6") int size) {
        FindearMatchingListResDto matchingList = matchingService.getFindearMatchingList(lostBoardId, page, size);
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "success", matchingList));
    }

    @GetMapping("/police/member/{memberId}")
    public ResponseEntity<?> getLost112BestMatching(@PathVariable Long memberId,
                                                    @RequestParam(required = false, defaultValue = "1") int page,
                                                    @RequestParam(required = false, defaultValue = "6") int size) {
        Lost112MatchingListResDto bestMatchings = matchingService.getLost112BestMatching(memberId, page, size);
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "success", bestMatchings));
    }

    @GetMapping("/police/board/{lostBoardId}")
    public ResponseEntity<?> getLost112MatchingList(@PathVariable Long lostBoardId,
                                                    @RequestParam(required = false, defaultValue = "1") int page,
                                                    @RequestParam(required = false, defaultValue = "6") int size) {
        Lost112MatchingListResDto matchingList = matchingService.getLost112MatchingList(lostBoardId, page, size);
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "success", matchingList));
    }

    // scrap
    @PostMapping("/police/scrap")
    public ResponseEntity<?> findLost112BoardListByAtcId(@RequestBody Lost112BoardListReqDto lost112BoardListReqDto) {
        List<Lost112AcquiredDto> scrapBoards = lost112AcquiredService.findListByAtcId(lost112BoardListReqDto);

        return ResponseEntity.ok(new SuccessResponse(HttpStatus.OK.value(), "success", scrapBoards));
    }

    // totalCount
    @GetMapping("/search/total")
    public ResponseEntity<?> getLost112TotalCount() {
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "success",
                        lost112AcquiredService.getNumOfLost112Acquired()));
    }

    // match 서버 api. 더미 응답이 필요한 API 1개만 있으므로. 테스트 환경에선 batch서버에서 처리
    @PostMapping("/process")
    public Map<String, Object> getDummyAutofillData(@RequestBody AutoFillReqDto autoFillReqDto) throws InterruptedException {
        Map<String, Object> aiGeneratedColumns = new HashMap<>();
        List<String> description = List.of("descriptionPart1", "descriptionPart2", "descriptionPart3");
        aiGeneratedColumns.put("category", "sampleCategoryName1");
        aiGeneratedColumns.put("color", "sampleColor1");
        aiGeneratedColumns.put("description", description);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "success");
        response.put("result", aiGeneratedColumns);

        Thread.sleep(2000);

        return response;
    }
}
