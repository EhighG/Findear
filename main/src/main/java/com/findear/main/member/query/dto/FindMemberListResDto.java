package com.findear.main.member.query.dto;

import com.findear.main.member.common.domain.Member;
import lombok.Getter;

@Getter
public class FindMemberListResDto {
    private Long memberId;
    private String phoneNumber;

    public FindMemberListResDto(Long memberId, String phoneNumber) {
        this.memberId = memberId;
        this.phoneNumber = phoneNumber;
    }

    public static FindMemberListResDto of(Member member) {
        return new FindMemberListResDto(member.getId(), member.getPhoneNumber());
    }
}
