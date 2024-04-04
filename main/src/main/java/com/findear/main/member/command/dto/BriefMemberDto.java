package com.findear.main.member.command.dto;

import com.findear.main.member.common.domain.Role;
import lombok.Getter;

@Getter
public class BriefMemberDto {
    private Long memberId;
    private String phoneNumber;
    private Role role;

    public BriefMemberDto(Long memberId, String phoneNumber, Role role) {
        this.memberId = memberId;
        this.phoneNumber = phoneNumber;
        this.role = role;
    }

    public BriefMemberDto(Long memberId, String phoneNumber) {
        this.memberId = memberId;
        this.phoneNumber = phoneNumber;
    }
}
