package com.findear.main.member.command.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NaverMemberInfoDto {
    private String uid;
    private String phoneNumber;
    private String age;
    private String gender;
}