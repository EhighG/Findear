package com.findear.main.member.command.dto;

import com.findear.main.member.common.domain.Agency;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.domain.Role;
import com.findear.main.member.common.dto.AgencyDto;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifyMemberResDto {
    private Long memberId;
    private Role role;
    private String phoneNumber;
    private LoginResAgencyDto agency;

    public static ModifyMemberResDto of(Member member) {
        Agency dbAgency = member.getAgency();
        ModifyMemberResDto result = ModifyMemberResDto.builder()
                .memberId(member.getId())
                .phoneNumber(member.getPhoneNumber())
                .role(member.getRole())
                .build();

        if (dbAgency != null) {
            result.agency = LoginResAgencyDto.builder()
                    .name(dbAgency.getName())
                    .address(dbAgency.getAddress())
                    .id(dbAgency.getId()).build();
        }
        return result;
    }
}
