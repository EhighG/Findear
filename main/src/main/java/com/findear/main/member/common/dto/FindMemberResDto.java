package com.findear.main.member.common.dto;

import com.findear.main.member.common.domain.Agency;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.domain.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FindMemberResDto {
    private Long memberId;
    @Setter
    private String phoneNumber;
    private Role role;
    private LocalDateTime joinedAt;
    private AgencyDto agency;

    public static FindMemberResDto of(Member member) {
        Agency agency = member.getAgency();
        return FindMemberResDto.builder()
                .memberId(member.getId())
                .phoneNumber(member.getPhoneNumber())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .agency(agency != null ? AgencyDto.builder()
                        .id(agency.getId())
                        .name(agency.getName())
                        .address(agency.getAddress())
                        .xPos(agency.getXPos())
                        .yPos(agency.getYPos())
                        .phoneNumber(agency.getPhoneNumber())
                        .build() : null)
                .build();
    }
}
