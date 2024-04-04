package com.findear.main.member.common.domain;


import lombok.Getter;

@Getter
public enum Role {
    NORMAL("ROLE_NORMAL"),
    MANAGER("ROLE_MANAGER");

    private final String value;

    Role(String value) {
        this.value = value;
    }
}

