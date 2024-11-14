package com.findear.main.member.command.service;

import com.findear.main.member.command.dto.*;

public interface MemberCommandService {
    String register(RegisterReqDto registerReqDto);
    ModifyMemberResDto changeToManager(Long memberId, RegisterAgencyReqDto registerAgencyReqDto);
    LoginResDto localLogin(LoginReqDto loginReqDto);
    LoginResDto afterSocialLogin(String authCode);
    void logout(Long memberId);
    ModifyMemberResDto modifyMember(Long memberId, ModifyMemberReqDto modifyMemberReqDto);
    void deleteMember(Long requestMemberId, Long targetMemberId);
    LoginResDto refreshAccessToken(String refreshToken, Long memberId);
}
