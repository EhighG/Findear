package com.findear.main.member.command.dto;

import com.findear.main.member.common.domain.Role;
import com.findear.main.member.common.dto.AgencyDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

//@ToString
@Getter @Setter
@NoArgsConstructor
public class ModifyMemberReqDto {

    private Long memberId;
    private Role role;
    private String phoneNumber;
    private AgencyDto agency;
}
