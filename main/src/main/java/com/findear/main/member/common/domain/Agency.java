package com.findear.main.member.common.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tbl_agency")
public class Agency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agency_id")
    private Long id;

    @OneToMany(mappedBy = "agency", fetch = FetchType.LAZY)
    private List<Member> memberList = new ArrayList<>();

    private String name;

    private Float xPos;

    private Float yPos;

    private String address;

    public void addMember(Member member) {
        if (memberList == null) {
            memberList = new ArrayList<>();
        }
        memberList.add(member);
    }

    public void removeMember(Member member) {
        memberList.remove(member);
    }
}
