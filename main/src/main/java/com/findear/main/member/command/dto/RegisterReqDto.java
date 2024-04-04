package com.findear.main.member.command.dto;

import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.domain.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class RegisterReqDto {

    private String naverUid;
    private String phoneNumber;
    private Role role;
    private String naverRefreshToken;

    public RegisterReqDto(String naverUid, String phoneNumber, String naverRefreshToken) {
        this.naverUid = naverUid;
        this.phoneNumber = phoneNumber;
        this.naverRefreshToken = naverRefreshToken;
        this.role = Role.NORMAL;
    }



    public Member toEntity() {
        return Member.builder()
                .naverUid(naverUid)
                .phoneNumber(phoneNumber)
                .naverRefreshToken(naverRefreshToken)
                .role(role)
                .build();
    }
}
