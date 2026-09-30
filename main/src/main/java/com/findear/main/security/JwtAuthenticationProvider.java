package com.findear.main.security;

import com.findear.main.common.profile.LocalProfile;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.dto.MemberDto;
import com.findear.main.member.query.service.MemberQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class JwtAuthenticationProvider {

    private final MemberQueryService memberQueryService;
    // 샘플 회원 인증(K-12)은 local 프로필에서만 쓴다. 기동 때 DB를 조회하지 않고, 쓸 때마다 조회한다 (개발용이라 비용 무시)
    private final boolean sampleAuthenticationEnabled;

    @Autowired
    public JwtAuthenticationProvider(MemberQueryService memberQueryService, Environment environment) {
        this(memberQueryService, LocalProfile.isActive(environment));
    }

    JwtAuthenticationProvider(MemberQueryService memberQueryService, boolean sampleAuthenticationEnabled) {
        this.memberQueryService = memberQueryService;
        this.sampleAuthenticationEnabled = sampleAuthenticationEnabled;
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

    // 토큰 없이 (local 프로필 전용). memberType: normal | manager — 그 역할의 첫 회원으로 인증된다
    public Authentication getSampleAuthentication(String memberType) {
        if (!sampleAuthenticationEnabled) {
            throw new AuthenticationServiceException("샘플 회원 인증은 local 프로필에서만 사용할 수 있습니다.");
        }
        Long sampleMemberId = findSampleMemberIds().get(memberType == null ? "" : memberType.trim().toLowerCase(Locale.ROOT));
        if (sampleMemberId == null) {
            throw new AuthenticationServiceException("test-member-type에 해당하는 샘플 회원이 없습니다.");
        }
        Member sampleMember = memberQueryService.internalFindById(sampleMemberId);

        return JwtAuthenticationToken.authenticated(sampleMember.getId(), // 후에 memberDto로 바꿀수도.
                "sampleAccessToken",
                Arrays.asList(new SimpleGrantedAuthority(sampleMember.getRole().getValue())));
    }

    private Map<String, Long> findSampleMemberIds() {
        Map<String, Long> sampleMemberIds = new HashMap<>();
        List<Member> sampleMemberList = memberQueryService.findFirstMembersPerGroup();
        for (Member sampleMember : sampleMemberList) {
            String roleType = sampleMember.getRole().getValue().substring("ROLE_".length()).toLowerCase(Locale.ROOT);
            sampleMemberIds.put(roleType, sampleMember.getId());
        }
        return sampleMemberIds;
    }
}
