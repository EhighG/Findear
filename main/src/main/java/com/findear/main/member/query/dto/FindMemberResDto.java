package com.findear.main.member.query.dto;

import com.findear.main.member.common.domain.Agency;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.domain.Role;
import com.findear.main.member.common.dto.AgencyDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class FindMemberResDto {
    private Long memberId;
    @Setter
    private String phoneNumber;
    private Role role;
//    private LocalDateTime joinedAt;
    private String joinedAt;
    private AgencyDto agency;

    public static FindMemberResDto of(Member member) {
        Agency agency = member.getAgency();
        return FindMemberResDto.builder()
                .memberId(member.getId())
                .phoneNumber(member.getPhoneNumber())
                .role(member.getRole())
                .joinedAt(member.getJoinedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .agency(agency != null ? AgencyDto.builder()
                        .id(agency.getId())
                        .name(agency.getName())
                        .address(agency.getAddress())
                        .xpos(agency.getXPos())
                        .ypos(agency.getYPos())
                        .build() : null)
                .build();
    }
}
