package com.findear.main.member.command.service;

import com.findear.main.Alarm.repository.NotificationRepository;
import com.findear.main.Alarm.service.NotificationService;
import com.findear.main.member.command.dto.*;
import com.findear.main.member.command.repository.AgencyCommandRepository;
import com.findear.main.member.command.repository.MemberCommandRepository;
import com.findear.main.member.common.domain.Agency;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.domain.Role;
import com.findear.main.member.common.dto.MemberDto;
import com.findear.main.member.query.repository.AgencyQueryRepository;
import com.findear.main.member.query.repository.MemberQueryRepository;
import com.findear.main.member.query.service.MemberQueryService;
import com.findear.main.security.JwtService;
import com.findear.main.security.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class MemberCommandService {

    private final MemberQueryService memberQueryService;
    private final MemberCommandRepository memberCommandRepository;
    private final AgencyQueryRepository agencyQueryRepository;
    private final AgencyCommandRepository agencyCommandRepository;
    private final RefreshTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NaverOAuthProvider naverOAuthProvider;
    private final MemberQueryRepository memberQueryRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final String NAVER_STATE = "test";


    /**
     * @param registerReqDto - phoneNumber, password
     * @return phoneNumber
     * @throws IllegalArgumentException - phoneNumber 중복
     */
    public String register(RegisterReqDto registerReqDto) {
        if (memberQueryService.checkDuplicate(registerReqDto.getPhoneNumber())) {
            throw new IllegalArgumentException("회원정보가 이미 있습니다.");
        }
        Member registerMember = Member.builder()
                .phoneNumber(registerReqDto.getPhoneNumber())
                // 테스트용. 실제 서비스 땐 소셜 로그인/간편 가입만 활용하므로, naverUid가 항상 input으로 들어옴.
                // 이에 맞추기 위함
                .naverUid("defaultNaverUid" + UUID.randomUUID())
                .role(Role.NORMAL)
                .build();
        Member savedMember = memberCommandRepository.save(registerMember);

        return savedMember.getPhoneNumber();
    }

    public ModifyMemberResDto changeToManager(Long memberId, RegisterAgencyReqDto registerAgencyReqDto) {
        Member member = memberQueryService.internalFindById(memberId);

        // 이미 관리자일 때 예외처리
        if (member.getRole() == Role.MANAGER) {
            throw new IllegalArgumentException("invalid / 회원정보 수정에서 처리돼야함");
        }
        // agency가 이미 존재할 때, 기존 것에 연결처리
        Optional<Agency> sameAgency = agencyQueryRepository.findByAddressAndName(registerAgencyReqDto.getAddress(), registerAgencyReqDto.getName());

//        Agency inputAgency = registerAgencyReqDto.toEntity();

        Agency agency = sameAgency.isPresent() ? sameAgency.get() : agencyCommandRepository.save(registerAgencyReqDto.toEntity());

        member.setAgencyAndRole(agency, Role.MANAGER);
        return ModifyMemberResDto.of(member);
    }

    public LoginResDto localLogin(LoginReqDto loginReqDto) {
        MemberDto memberDto = memberQueryService.findByPhoneNumber(loginReqDto.getPhoneNumber());

        memberQueryService.validMemberNotDeleted(memberDto.toEntity()); // 향후, deleted면 회원 복구 로직으로 변경

//        verifyPassword(loginReqDto.getPassword(), memberDto);

        LoginResDto loginResDto = new LoginResDto();
        loginResDto.setMemberAndAgency(memberDto);
        makeTokens(memberDto.getId(), loginResDto);
        return loginResDto;
    }

    public LoginResDto login(String authCode) {
        NaverAccessTokenResponse accessTokenResponse = naverOAuthProvider.getAccessToken(authCode, NAVER_STATE);
        String naverRefreshToken = accessTokenResponse.getRefreshToken();

        NaverMemberInfoDto memberInfo = naverOAuthProvider.getMemberInfo(accessTokenResponse.getAccessToken());

        Optional<Member> memberOptional = memberQueryRepository.findByPhoneNumber(memberInfo.getPhoneNumber());
        Member member;
        // 회원정보 등록 or 업데이트
        if (memberOptional.isEmpty()) {
            // 회원정보 등록
            RegisterReqDto registerReqDto = new RegisterReqDto(memberInfo.getUid(), memberInfo.getPhoneNumber(), naverRefreshToken);
            member = memberCommandRepository.save(registerReqDto.toEntity());
        } else {
            // 업데이트
            member = memberOptional.get();
            member.updateNaverInfo(memberInfo.getUid(), memberInfo.getPhoneNumber(), naverRefreshToken);
        }
        // 자체 access/refresh token 발행
        LoginResDto loginResDto = new LoginResDto();
        makeTokens(member.getId(), loginResDto);
        loginResDto.setMemberAndAgency(MemberDto.of(member));
        return loginResDto;
    }

    public void logout(Long memberId) {

        // refresh token 삭제
        tokenRepository.deleteRefreshToken(memberId);

        // fcm 토큰 삭제
        notificationService.deleteNotification(memberId);
    }

    /**
     * 입력 가능한 파라미터들 값을 미리 내려준 후, 모두 들어있는 값을 받는다.
     * @return
     */
    public ModifyMemberResDto modifyMember(Long memberId, ModifyMemberReqDto modifyMemberReqDto) {
        if (!memberId.equals(modifyMemberReqDto.getMemberId())) {
            throw new RuntimeException("다른 유저 정보수정; 403 처리");
        }
        Member member = memberQueryService.internalFindById(modifyMemberReqDto.getMemberId());

        // update - 항상 내려 주고, 전부 받는 게 자연스럽다.
        // 케이스별 구분, Agency 처리
        if (member.getRole() == Role.MANAGER) {
            if (modifyMemberReqDto.getRole() == Role.MANAGER) {
                managerToManager(member, modifyMemberReqDto);
            } else if (modifyMemberReqDto.getRole() == Role.NORMAL) {
                managerToNormal(member, modifyMemberReqDto);
            }
        } else if (member.getRole() == Role.NORMAL) {
            if (modifyMemberReqDto.getRole() == Role.MANAGER) {
                normalToManager(member, modifyMemberReqDto);
            }
        }
        // 나머지 member 정보 변경 : 일단은 전화번호만
        member.changePhoneNumber(modifyMemberReqDto.getPhoneNumber());
//        return new BriefMemberDto(member.getId(), member.getPhoneNumber(), member.getRole());
        return ModifyMemberResDto.of(member);
    }

    public void deleteMember(Long requestMemberId, Long targetMemberId) {
        Member member = memberQueryService.internalFindById(targetMemberId);
        if (!targetMemberId.equals(requestMemberId)) {
            throw new AuthenticationServiceException("권한이 없습니다.");
        }
        member.withdraw();
    }

//    public List<Member> findMembers(String keyword) {
//        List<Member> members = memberRepository.findAll();
//        return members.stream().filter((member) -> member.getPhoneNumber().contains(keyword)).collect(Collectors.toList());
//    }

    /**
     * cases :
     * 1. accessToken 만료, refreshToken은 있는 경우 -> redis 확인 후 재발급
     * 2. accessToken, refreshToken 둘 다 없고, 네이버 refreshToken은 있는 경우 -> 네이버에서 회원정보 업데이트 후, 재발급
     * 3. 네이버 refreshToken까지, 셋 다 없는 경우 -> 에러 반환해서 네이버 로그인/인가부터 다시 하게
     */
    public LoginResDto refreshAccessToken(String refreshToken, Long memberId) {
        Optional<Member> memberOptional = memberQueryRepository.findById(memberId);
        Member member = memberOptional
                .orElseThrow(() -> new AuthenticationServiceException("잘못된 member ID"));
        Optional<String> storedTokenOptional = tokenRepository.findByMemberId(memberId);

        if (jwtService.isExpired(refreshToken) || storedTokenOptional.isEmpty()) {
            /*
            DB에서 naver Refresh token 꺼내와서,
            accessToken 재발급 받아오고,
            그걸로 회원정보 받아오고, 업데이트
            후, 자체 access/refresh token 생성해서
            redis 저장 후, 반환
             */
            String naverRefreshToken = member.getNaverRefreshToken();
            if (naverRefreshToken == null) {
                log.info("DB에 refreshToken이 없음. 재로그인 필요");
                throw new AuthenticationServiceException("DB에 refreshToken이 없음. 재로그인 필요");
            }
            NaverAccessTokenResponse accessTokenResponse = naverOAuthProvider.refreshAccessToken(naverRefreshToken);
            NaverMemberInfoDto memberInfo = naverOAuthProvider.getMemberInfo(accessTokenResponse.getAccessToken());
            member.updateNaverInfo(memberInfo.getUid(), memberInfo.getPhoneNumber(), naverRefreshToken);

            String localAccessToken = jwtService.createAccessToken(memberId);
            String localRefreshToken = jwtService.createRefreshToken(memberId);

            tokenRepository.save(memberId, localRefreshToken);
            LoginResDto loginResDto = new LoginResDto(localAccessToken, localRefreshToken);
            loginResDto.setMemberAndAgency(MemberDto.of(member));
            return loginResDto;
        }
        String storedRefreshToken = storedTokenOptional.get();
        if (!storedRefreshToken.equals(refreshToken)) {
            throw new AuthenticationServiceException("잘못된 refreshToken");
        }
        // refreshToken 이 잘 있는 경우
        String newAccessToken = jwtService.createAccessToken(memberId);
        LoginResDto loginResDto = new LoginResDto();
        loginResDto.setMemberAndAgency(MemberDto.of(member));
        loginResDto.setAccessToken(newAccessToken);
        return loginResDto;
    }

    private Agency saveAgency(Agency agency) {
        Optional<Agency> optionalAgency = agencyQueryRepository.findByAddressAndName(agency.getAddress(),
                agency.getName());

        Agency savedAgency = optionalAgency.isPresent() ? optionalAgency.get() : agencyQueryRepository.save(agency);

        return savedAgency;
    }

    private void normalToManager(Member oldMember, ModifyMemberReqDto modifyReqDto) {
        Agency agency = saveAgency(modifyReqDto.getAgency().toEntity());
        oldMember.setAgencyAndRole(agency, Role.MANAGER);
    }

    private void managerToManager(Member oldMember, ModifyMemberReqDto modifyReqDto) {
        Agency agency = saveAgency(modifyReqDto.getAgency().toEntity());
        oldMember.changeAgency(agency);
    }

    private void managerToNormal(Member oldMember, ModifyMemberReqDto modifyReqDto) {
        oldMember.toNormal();
    }

    private LoginResDto makeTokens(Long memberId, LoginResDto loginResDto) {
        String accessToken = jwtService.createAccessToken(memberId);
        String refreshToken = jwtService.createRefreshToken(memberId);
        tokenRepository.save(memberId, refreshToken);

        loginResDto.setAccessToken(accessToken);
        loginResDto.setRefreshToken(refreshToken);
        return loginResDto;
    }
}
