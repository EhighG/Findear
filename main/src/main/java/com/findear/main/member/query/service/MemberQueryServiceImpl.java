package com.findear.main.member.query.service;

import com.findear.main.member.command.service.NaverOAuthProvider;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.dto.*;
import com.findear.main.member.query.dto.FindMemberListResDto;
import com.findear.main.member.query.dto.FindMemberResDto;
import com.findear.main.member.query.repository.MemberQueryRepository;
import com.findear.main.security.RefreshTokenRepository;
import com.findear.main.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class MemberQueryServiceImpl {

    private final MemberQueryRepository memberQueryRepository;
    private final RefreshTokenRepository tokenRepository;
    private final JwtService jwtService;
    private final NaverOAuthProvider naverOAuthProvider;

    public static Long getAuthenticatedMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!authentication.isAuthenticated()) {
            throw new IllegalStateException("로그인되지 않은 상태입니다.");
        }
        return (Long) authentication.getPrincipal();
    }

    public List<Member> findFirstMembersPerGroup() {
        return memberQueryRepository.findFirstMembersGroupByRoleOrderById();
    }

    public MemberDto findByPhoneNumber(String phoneNumber) {
        Member member = memberQueryRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UsernameNotFoundException("회원정보가 존재하지 않습니다."));
        return MemberDto.of(member);
    }

    public boolean checkDuplicate(String phoneNumber) {
        if (phoneNumber.charAt(0) == '"') {
            phoneNumber = phoneNumber.substring(1, phoneNumber.length() - 1);
        }
        return memberQueryRepository.findByPhoneNumber(phoneNumber).isPresent();
    }

    public Member internalFindById(Long memberId) {

        Member member = memberQueryRepository.findByIdWithAgency(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원정보가 존재하지 않습니다."));


        validMemberNotDeleted(member);
        return member;
    }

    public FindMemberResDto findMemberById(Long targetMemberId, Long requestMemberId) {
        Member member = internalFindById(targetMemberId);

        FindMemberResDto foundMember = FindMemberResDto.of(member);

        if (!foundMember.getMemberId().equals(requestMemberId)) {
            foundMember.setPhoneNumber(blindPhoneNumber(foundMember.getPhoneNumber()));
        }
        return foundMember;
    }

    public List<FindMemberListResDto> findMembers(String keyword) {
        List<Member> members = memberQueryRepository.findAll();
        return members.stream()
                .filter(member -> member.getPhoneNumber().contains(keyword)
                && (member.getWithdrawalYn() == null || !member.getWithdrawalYn()))
                .map(FindMemberListResDto::of)
                .collect(Collectors.toList());
    }

    public MemberDto verifyAccessToken(String accessToken) {
        if (jwtService.isExpired(accessToken)) {
            throw new AuthenticationServiceException("401 unauthorized");
        }
        Long memberId = jwtService.getMemberId(accessToken);
        String refreshToken = tokenRepository.findByMemberId(memberId)
                .orElseThrow(() -> new AuthenticationServiceException("401 unauthorized"));

        if (!jwtService.getMemberId(refreshToken).equals(memberId)) {
            throw new AuthenticationServiceException("401 unauthorized");
        }

        Member member = memberQueryRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원정보가 존재하지 않습니다."));

        return MemberDto.of(member);
    }

//    public MemberDto refreshAccessToken(String refreshToken, Long memberId) {
//        if (jwtService.isExpired(refreshToken)) { // redis에서도 이미 사라졌다.
//            /*
//            DB에서 naver Refresh token 꺼내와서,
//            accessToken 재발급 받아오고,
//            그걸로 회원정보 받아오고, 업데이트
//            후, 자체 access/refresh token 생성해서
//            redis 저장 후, 반환
//             */
//            Optional<Member> memberOptional = memberQueryRepository.findById(memberId);
//            String naverRefreshToken = memberOptional
//                    .orElseThrow(() -> new AuthenticationServiceException("잘못된 member ID"))
//                    .getNaverRefreshToken();
//            if (naverRefreshToken == null) {
//                log.info("DB에 refreshToken이 없음. 재로그인 필요");
//                throw new AuthenticationServiceException("DB에 refreshToken이 없음. 재로그인 필요");
//            }
//            NaverAccessTokenResponse accessTokenResponse = naverOAuthProvider.refreshAccessToken(refreshToken);
//            NaverMemberInfoDto memberInfo = naverOAuthProvider.getMemberInfo(accessTokenResponse.getAccessToken());
//            Member member = memberOptional.get();
//            member.updateNaverInfo(memberInfo.getUid(), memberInfo.getPhoneNumber(), refreshToken);
//
//            String localAccessToken = jwtService.createAccessToken(memberId);
//            String localRefreshToken = jwtService.createRefreshToken(memberId);
//
//        }
//
//        Long extractedMemberId = jwtService.getMemberId(refreshToken);
//        String storedToken = tokenRepository.findByMemberId(extractedMemberId)
//                .orElseThrow(() -> new AuthenticationServiceException("401 unauthorized"));
//
//        if (!refreshToken.equals(storedToken)) {
//            throw new AuthenticationServiceException("401 unauthorized");
//        }
//        Member member = memberQueryRepository.findById(extractedMemberId)
//                .orElseThrow(() -> new AuthenticationServiceException("401 unauthorized"));
//        return MemberDto.of(member);
//    }

    public void validMemberNotDeleted(Member member) {
        if (member.getWithdrawalYn() != null && member.getWithdrawalYn()) {
            throw new UsernameNotFoundException("회원정보가 존재하지 않습니다.");
        }
    }

    private String blindPhoneNumber(String phoneNumber) {
        int len = phoneNumber.length();
        if (len <= 4) return phoneNumber;
        return "****" + phoneNumber.substring(len - 4);
    }
}
