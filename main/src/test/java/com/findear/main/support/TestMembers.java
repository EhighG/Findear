package com.findear.main.support;

import com.findear.main.member.command.repository.MemberCommandRepository;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.domain.Role;
import com.findear.main.security.JwtService;
import com.findear.main.security.RefreshTokenRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** 통합 테스트용 회원·토큰 만들기. 회원은 테스트 트랜잭션 안에서 저장하므로 테스트가 끝나면 롤백된다. */
@Component
public class TestMembers {

    private final MemberCommandRepository memberRepository;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    public TestMembers(MemberCommandRepository memberRepository, JwtService jwtService,
                       RefreshTokenRepository refreshTokenRepository) {
        this.memberRepository = memberRepository;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public Member newMember(Role role) {
        return memberRepository.saveAndFlush(Member.builder()
                .naverUid("test-uid-" + UUID.randomUUID())
                .phoneNumber("010" + ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999))
                .role(role)
                .build());
    }

    /** access token을 만들고, 서버가 요구하는 refresh token도 Redis에 저장한다 (로그인 흐름과 같은 상태). */
    public String loginToken(Member member) {
        refreshTokenRepository.save(member.getId(), jwtService.createRefreshToken(member.getId()));
        return jwtService.createAccessToken(member.getId());
    }

    public void logout(Member member) {
        refreshTokenRepository.deleteRefreshToken(member.getId());
    }
}
