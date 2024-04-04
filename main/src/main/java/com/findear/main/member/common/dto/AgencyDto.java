package com.findear.main.member.common.dto;

import com.findear.main.member.common.domain.Agency;
import com.findear.main.member.common.domain.Member;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgencyDto {
    private Long id;
    private String name;
//    private List<Member> memberList = new ArrayList<>();
    private String address;
    private Float xpos;
    private Float ypos;

    public static AgencyDto of(Agency agency) {
        return AgencyDto.builder()
                .id(agency.getId())
                .name(agency.getName())
//                .memberList(agency.getMemberList())
                .address(agency.getAddress())
                .xpos(agency.getXPos())
                .ypos(agency.getYPos())
                .build();
    }

    public Agency toEntity() {
        return Agency.builder()
                .id(id)
                .name(name)
//                .memberList(memberList)
                .address(address)
                .xPos(xpos)
                .yPos(ypos)
                .build();
    }
}
