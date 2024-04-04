package com.findear.main.security;

import com.findear.main.member.common.dto.MemberDto;
import com.findear.main.member.command.service.MemberCommandService;
import com.findear.main.member.query.service.MemberQueryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class JwtAuthenticationProvider {

    private final MemberQueryService memberQueryService;

    public JwtAuthenticationProvider(MemberQueryService memberQueryService) {
        this.memberQueryService = memberQueryService;
    }

    /**
     * param : unauthenticated 이고, accessToken만 담고 있는 객체
     * return : authenticated이고, memberId가 추가된 객체
     */
    public Authentication authenticateAccessToken(String accessToken) {
        MemberDto memberDto = memberQueryService.verifyAccessToken(accessToken);

        return JwtAuthenticationToken.authenticated(memberDto.getId(), accessToken,
                Arrays.asList(new SimpleGrantedAuthority(memberDto.getRole().getValue())));
    }
//
//    /**
//     * param : unauthenticated 이고, refreshToken만 담고 있는 객체
//     * return : authenticated이고, memberId가 추가된 객체
//     */
//    public Authentication authenticationRefreshToken(String refreshToken, Long memberId) {
//        MemberDto memberDto = memberQueryService.verifyRefreshToken(refreshToken, memberId);
//
//        return JwtAuthenticationToken.authenticated(memberId, refreshToken,
//                Arrays.asList(new SimpleGrantedAuthority(memberDto.getRole().getValue())));
//    }
}
