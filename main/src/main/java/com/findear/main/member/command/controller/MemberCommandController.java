package com.findear.main.member.command.controller;

import com.findear.main.common.response.SuccessResponse;
import com.findear.main.member.command.dto.*;
import com.findear.main.member.command.service.MemberCommandService;
import com.findear.main.member.command.service.MemberCommandServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequestMapping("/members")
@RestController
public class MemberCommandController {

    private final MemberCommandService memberCommandService;

    public MemberCommandController(MemberCommandServiceImpl memberCommandService) {
        this.memberCommandService = memberCommandService;
    }

    @PatchMapping("/{memberId}/role")
    public ResponseEntity<?> changeToManager(@PathVariable Long memberId,
                                             @AuthenticationPrincipal Long requestMemberId,
                                             @RequestBody RegisterAgencyReqDto registerAgencyReqDto) {
        // 자기 기관을 등록하는 요청만 허용한다 (다른 회원을 관리자로 바꿀 수 없다)
        if (!memberId.equals(requestMemberId)) {
            throw new AuthorizationServiceException("본인의 정보만 변경할 수 있습니다.");
        }
        return ResponseEntity.ok(new SuccessResponse(HttpStatus.OK.value(), "변경되었습니다.",
                memberCommandService.changeToManager(memberId, registerAgencyReqDto)));
    }

    // 소셜 로그인 / authCode를 갖고 요청
    @GetMapping("/login")
    public ResponseEntity<?> redirectReqWithCode(@RequestParam(required = false) String code,
                                    @RequestParam(name = "error", required = false) String errorCode,
                                    @RequestParam(name = "error_description", required = false) String errorDescription,
                                    @RequestParam(required = false) String state, HttpServletResponse response) throws IOException {
        Map<String, String> result = new HashMap<>();
        if (errorCode != null) {
            result.put("errorCode", errorCode);
            result.put("errorDescription", errorDescription);
            log.info("인가코드 발급 후 리다이렉트 중 오류 / errCode = " + errorCode + "\nerrMessage = " + errorDescription);
            return new ResponseEntity<>(result, HttpStatus.UNAUTHORIZED);
        }
//        // code가 잘 왔다면
        result.put("code", code);
        return ResponseEntity
                .ok().body(result);
    }

    @GetMapping("/after-login")
    public ResponseEntity<?> login(@RequestParam String code) {
        return ResponseEntity
                .ok(memberCommandService.afterSocialLogin(code));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal Long memberId) {
        memberCommandService.logout(memberId);
        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(),
                "로그아웃되었습니다."));
    }

    @PatchMapping("/{memberId}")
    public ResponseEntity<?> modifyMember(@AuthenticationPrincipal Long memberId,
                                          @RequestBody ModifyMemberReqDto modifyMemberReqDto) {

        return ResponseEntity
                .ok()
                .body(new SuccessResponse(HttpStatus.OK.value(), "변경되었습니다.",
                        memberCommandService.modifyMember(memberId, modifyMemberReqDto)));
    }

    @PatchMapping("/{targetMemberId}/delete")
    public ResponseEntity<?> deleteMember(@AuthenticationPrincipal Long requestMemberId,
                                          @PathVariable Long targetMemberId) {
        memberCommandService.deleteMember(requestMemberId, targetMemberId);
        return ResponseEntity
                .ok()
                .body(new SuccessResponse(HttpStatus.OK.value(), "탈퇴되었습니다."));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<?> refreshAccessToken(HttpServletRequest request, @RequestBody Long memberId) {
        String refreshToken = request.getHeader("refresh-token");
        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(),
                "발급되었습니다.",
                memberCommandService.refreshAccessToken(refreshToken, memberId)));
    }
}
