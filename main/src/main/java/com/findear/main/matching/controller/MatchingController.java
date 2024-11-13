package com.findear.main.matching.controller;

import com.findear.main.common.response.SuccessResponse;
import com.findear.main.matching.service.MatchingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RequiredArgsConstructor
@RequestMapping("/matchings")
@RestController
public class MatchingController {

    private final MatchingService matchingService;

    @GetMapping("/findear/bests")
    public ResponseEntity<?> getFindearBestMatchings(@AuthenticationPrincipal Long memberId,
                                                     @RequestParam(required = false, defaultValue = "1") Integer pageNo,
                                                     @RequestParam(required = false, defaultValue = "6") Integer size) {
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "조회에 성공했습니다.",
                        matchingService.getFindearBestsResponse(memberId, pageNo, size)));
    }

    @GetMapping("/findear/total")
    public ResponseEntity<?> getFindearMatchingList(@RequestParam Long lostBoardId,
                                                    @RequestParam(required = false, defaultValue = "1") Integer pageNo,
                                                    @RequestParam(required = false, defaultValue = "6") Integer size) {
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "조회에 성공했습니다.",
                        matchingService.getFindearMatchingsResponse(lostBoardId, pageNo, size)));
    }

    @GetMapping("/lost112/bests")
    public ResponseEntity<?> getLost112BestMatchings(@AuthenticationPrincipal Long memberId,
                                                     @RequestParam(required = false, defaultValue = "1") Integer pageNo,
                                                     @RequestParam(required = false, defaultValue = "6") Integer size) {
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "조회에 성공했습니다.",
                        matchingService.getLost112BestsResponse(memberId, pageNo, size)));
    }

    @GetMapping("/lost112/total")
    public ResponseEntity<?> getLost112MatchingList(@RequestParam Long lostBoardId,
                                                    @RequestParam(required = false, defaultValue = "1") Integer pageNo,
                                                    @RequestParam(required = false, defaultValue = "6") Integer size) {
        return ResponseEntity
                .ok(new SuccessResponse(HttpStatus.OK.value(), "조회에 성공했습니다.",
                        matchingService.getLost112MatchingsResponse(lostBoardId, pageNo, size)));
    }
}
