package com.findear.main.member.command.dto;

import com.findear.main.member.command.dto.BriefMemberDto;
import com.findear.main.member.common.domain.Agency;
import com.findear.main.member.common.dto.AgencyDto;
import com.findear.main.member.common.dto.MemberDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
public class LoginResDto {

    @Setter
    private String accessToken;

    @Setter
    private String refreshToken;

    private BriefMemberDto member;

    private LoginResAgencyDto agency;

    public LoginResDto(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public void setMemberAndAgency(MemberDto memberDto) {
        this.member = new BriefMemberDto(memberDto.getId(), memberDto.getPhoneNumber(), memberDto.getRole());
        Agency agency = memberDto.getAgency();
        if (agency == null) {
            return;
        }
        this.agency = LoginResAgencyDto.builder()
                .id(agency.getId())
                .name(agency.getName())
                .address(agency.getAddress())
                .build();
    }
}
