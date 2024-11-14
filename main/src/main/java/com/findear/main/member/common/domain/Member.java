package com.findear.main.member.common.domain;

import com.findear.main.Alarm.common.domain.Alarm;
import com.findear.main.Alarm.common.domain.Notification;
import com.findear.main.board.common.domain.Board;
import com.findear.main.board.common.domain.Lost112Scrap;
import com.findear.main.board.common.domain.Scrap;
import com.findear.main.message.common.domain.MessageRoom;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "tbl_member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false)
    private String naverUid;

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<MessageRoom> messageRoomList = new ArrayList<>();

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<Board> boardList = new ArrayList<>();

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<Scrap> scrapList = new ArrayList<>();

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<Lost112Scrap> lost112ScrapList = new ArrayList<>();

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<Alarm> alarmList = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private Agency agency;

    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY)
    private Notification notification;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private String phoneNumber;

    @CreatedDate
    private LocalDateTime joinedAt;

    private LocalDateTime withdrawalAt;

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("0")
    private Boolean withdrawalYn = false;

    private String naverRefreshToken;

    public void setAgencyAndRole(Agency agency, Role role) {
        this.agency = agency;
        this.role = role;
        agency.addMember(this);
    }

    public void changeAgency(Agency agency) {
        this.agency.removeMember(this);
        agency.addMember(this);
        this.agency = agency;
    }

    public void changePhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void toNormal() {
        this.agency.removeMember(this);
        this.agency = null;
        this.role = Role.NORMAL;
    }

    public void withdraw() {
        this.withdrawalYn = true;
        this.withdrawalAt = LocalDateTime.now();
    }

    public void updateNaverInfo(String naverUid, String phoneNumber, String naverRefreshToken) {
        this.naverUid = naverUid;
        this.phoneNumber = phoneNumber;
        this.naverRefreshToken = naverRefreshToken;
    }
}

