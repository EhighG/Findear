package com.findear.stubbatchserver.controller;


import com.findear.stubbatchserver.Lost112BoardListReqDto;
import com.findear.stubbatchserver.common.SuccessResponse;
import com.findear.stubbatchserver.dto.FindearMatchingListResDto;
import com.findear.stubbatchserver.dto.Lost112BoardListResDto;
import com.findear.stubbatchserver.dto.Lost112MatchingListResDto;
import com.findear.stubbatchserver.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/")
public class MatchingController {

    private final MatchingService matchingService;

    @GetMapping("/findear/member/{memberId}")
    public ResponseEntity<?> getFindearBestMatching(@PathVariable Long memberId,
                                                    @RequestParam(required = false, defaultValue = "1") int page,
                                                    @RequestParam(required = false, defaultValue = "6") int size) {
        FindearMatchingListResDto bestMatchings = null;

        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "success", bestMatchings));
    }

    @GetMapping("/findear/board/{lostBoardId}")
    public ResponseEntity<?> getFindearMatchingList(@PathVariable Long lostBoardId,
                                                    @RequestParam(required = false, defaultValue = "1") int page,
                                                    @RequestParam(required = false, defaultValue = "6") int size) {
        FindearMatchingListResDto matchingList = null;
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "success", matchingList));
    }

    @GetMapping("/police/member/{memberId}")
    public ResponseEntity<?> getLost112BestMatching(@PathVariable Long memberId,
                                                    @RequestParam(required = false, defaultValue = "1") int page,
                                                    @RequestParam(required = false, defaultValue = "6") int size) {
        Lost112MatchingListResDto bestMatchings = null;
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "success", bestMatchings));
    }

    @GetMapping("/police/board/{lostBoardId}")
    public ResponseEntity<?> getLost112MatchingList(@PathVariable Long lostBoardId,
                                                    @RequestParam(required = false, defaultValue = "1") int page,
                                                    @RequestParam(required = false, defaultValue = "6") int size) {
        Lost112MatchingListResDto matchingList = null;
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "success", matchingList));
    }

    // scrap
    @PostMapping("/scrap")
    public ResponseEntity<?> findLost112BoardListByAtcIds(@RequestBody Lost112BoardListReqDto lost112BoardListReqDto) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        List<Lost112BoardListResDto> scrapBoards = null;

        return ResponseEntity.ok(new SuccessResponse(HttpStatus.OK.value(), "success", scrapBoards));

    }

    // totalCount
    @GetMapping("/search/total")
    public ResponseEntity<?> getLost112TotalCount() {
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "success",
                        matchingService.getNumOfLost112Acquired()));
    }
}
