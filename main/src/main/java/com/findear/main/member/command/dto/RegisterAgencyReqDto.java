package com.findear.main.member.command.dto;

import com.findear.main.member.common.domain.Agency;
import com.findear.main.member.common.domain.Member;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
public class RegisterAgencyReqDto {
    private String name;
    private String address;
    private String xpos;
    private String ypos;
//    private Float xPosValue;
//    private Float yPosValue;

    public Agency toEntity() {
        return Agency.builder()
                .name(name)
                .address(address)
                .xPos(Float.parseFloat(xpos))
                .yPos(Float.parseFloat(ypos))
                .build();
    }

}
