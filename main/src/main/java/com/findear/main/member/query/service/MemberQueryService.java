package com.findear.main.member.query.service;

import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.dto.MemberDto;
import com.findear.main.member.query.dto.FindMemberListResDto;
import com.findear.main.member.query.dto.FindMemberResDto;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public interface MemberQueryService {
    List<Member> findFirstMembersPerGroup();
    MemberDto findByPhoneNumber(String phoneNumber);
    boolean checkDuplicate(String phoneNumber);
    Member internalFindById(Long memberId);
    FindMemberResDto findMemberById(Long targetMemberId, Long requestMemberId);
    List<FindMemberListResDto> findMembers(String keyword);
    MemberDto verifyAccessToken(String accessToken);
    void validMemberNotDeleted(Member member);


    static Long getAuthenticatedMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!authentication.isAuthenticated()) {
            throw new IllegalStateException("로그인되지 않은 상태입니다.");
        }
        return (Long) authentication.getPrincipal();
    }
}
