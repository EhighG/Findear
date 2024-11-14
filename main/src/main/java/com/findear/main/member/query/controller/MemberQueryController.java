package com.findear.main.member.query.controller;

import com.findear.main.common.response.SuccessResponse;
import com.findear.main.member.query.dto.FindMemberResDto;
import com.findear.main.member.query.service.MemberQueryServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/members")
@RestController
public class MemberQueryController {

    private final MemberQueryServiceImpl memberQueryService;

    public MemberQueryController(MemberQueryServiceImpl memberQueryService) {
        this.memberQueryService = memberQueryService;
    }

    @PostMapping("/duplicate")
    public ResponseEntity<?> checkDuplicate(@RequestBody String phoneNumber) {
        return ResponseEntity
                .ok()
                .body(new SuccessResponse(HttpStatus.OK.value(), "요청에 성공하였습니다.",
                        memberQueryService.checkDuplicate(phoneNumber)));
    }

    @GetMapping("/{targetMemberId}")
    public ResponseEntity<?> findById(@PathVariable Long targetMemberId,
                                      @AuthenticationPrincipal Long requestMemberId) {
        FindMemberResDto foundMember = memberQueryService.findMemberById(targetMemberId, requestMemberId);
        return ResponseEntity
                .ok()
                .body(new SuccessResponse(HttpStatus.OK.value(), "요청에 성공하였습니다.",
                        foundMember));
    }

    @GetMapping("/token-check")
    public ResponseEntity<?> checkAccessToken() {
        return ResponseEntity
                .ok()
                .body(new SuccessResponse(HttpStatus.OK.value(), "유효한 accessToken입니다."));
    }

    @GetMapping
    public ResponseEntity<?> findMembers(@RequestParam(required = false) String keyword) {
        return ResponseEntity
                .ok()
                .body(new SuccessResponse(HttpStatus.OK.value(), "요청에 성공하였습니다.",
                        memberQueryService.findMembers(keyword)));
    }
}
