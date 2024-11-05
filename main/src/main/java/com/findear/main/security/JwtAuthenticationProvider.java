package com.findear.main.security;

import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.dto.MemberDto;
import com.findear.main.member.command.service.MemberCommandService;
import com.findear.main.member.query.dto.FindMemberResDto;
import com.findear.main.member.query.service.MemberQueryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationProvider {

    private final MemberQueryService memberQueryService;
    private Map<String, Long> sampleMemberIds;

    public JwtAuthenticationProvider(MemberQueryService memberQueryService) {
        this.memberQueryService = memberQueryService;
        sampleMemberIds = new HashMap<>();
        // 샘플 멤버 초기화
        List<Member> sampleMemberList = memberQueryService.findFirstMembersPerGroup();
        for (Member sampleMember : sampleMemberList) {
            String roleType = sampleMember.getRole().getValue().substring("ROLE_".length()).toLowerCase();
            sampleMemberIds.put(roleType, sampleMember.getId());
        }
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

    // 토큰 없이
    public Authentication getSampleAuthentication(String memberType) {
        FindMemberResDto sampleMember = FindMemberResDto.of(
                memberQueryService.internalFindById(sampleMemberIds.get(memberType))
        );

        return JwtAuthenticationToken.authenticated(sampleMember.getMemberId(), // 후에 memberDto로 바꿀수도.
                "sampleAccessToken",
                Arrays.asList(new SimpleGrantedAuthority(sampleMember.getRole().getValue())));
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
