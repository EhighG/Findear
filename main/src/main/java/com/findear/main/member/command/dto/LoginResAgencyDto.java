package com.findear.main.member.command.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResAgencyDto {
    private Long id;
    private String name;
    private String address;

    @Builder
    public LoginResAgencyDto(Long id, String name, String address) {
        this.id = id;
        this.name = name;
        this.address = address;
    }
}
