package com.findear.main.member.command.controller;

import com.findear.main.common.profile.LocalProfile;
import com.findear.main.common.response.SuccessResponse;
import com.findear.main.member.command.dto.LoginReqDto;
import com.findear.main.member.command.dto.RegisterReqDto;
import com.findear.main.member.command.service.MemberCommandService;
import com.findear.main.member.command.service.MemberCommandServiceImpl;
import com.findear.main.member.query.service.MemberQueryService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개발용 가입·로그인 (K-06)과 회원 전화번호 목록 검색(레거시 프론트가 쓰지 않음). local 프로필에서만 등록된다 (D-26).
 * 배포에서는 이 컨트롤러가 없으므로 전화번호만으로 로그인하거나 가입할 수 없다 (로그인은 소셜 로그인뿐).
 */
@Profile(LocalProfile.NAME)
@RequestMapping("/members")
@RestController
public class LocalMemberController {

    private final MemberCommandService memberCommandService;
    private final MemberQueryService memberQueryService;

    public LocalMemberController(MemberCommandServiceImpl memberCommandService, MemberQueryService memberQueryService) {
        this.memberCommandService = memberCommandService;
        this.memberQueryService = memberQueryService;
    }

    // 전화번호만으로 가입 (naverUid는 임의 값)
    @PostMapping
    public ResponseEntity<?> register(@RequestBody RegisterReqDto registerReqDto) {
        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(),
                "가입되었습니다.",
                memberCommandService.register(registerReqDto)));
    }

    // 전화번호만으로 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginReqDto loginReqDto) {
        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(),
                "로그인에 성공하였습니다.",
                memberCommandService.localLogin(loginReqDto)));
    }

    // 전화번호에 keyword가 들어간 회원 목록. keyword가 없으면 전체 (탈퇴 회원 제외)
    @GetMapping
    public ResponseEntity<?> findMembers(@RequestParam(required = false) String keyword) {
        return ResponseEntity
                .ok()
                .body(new SuccessResponse(HttpStatus.OK.value(), "요청에 성공하였습니다.",
                        memberQueryService.findMembers(keyword)));
    }
}
