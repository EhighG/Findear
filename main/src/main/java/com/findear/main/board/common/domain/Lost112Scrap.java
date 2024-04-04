package com.findear.main.board.common.domain;

import com.findear.main.member.common.domain.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tbl_lost112_scrap")
public class Lost112Scrap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lost112_scrap_id")
    private Long id;

    @Column(name = "lost112_atc_id")
    @JoinColumn(name = "lost112_atc_id")
    private String lost112AtcId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public Lost112Scrap(String lost112AtcId, Member member) {
        this.lost112AtcId = lost112AtcId;
        this.member = member;
    }
}
