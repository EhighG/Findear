//package com.findear.batch.alarm.domain;
//
//import com.findear.batch.ours.domain.Member;
//import lombok.AccessLevel;
//import lombok.Builder;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//
//import javax.persistence.*;
//
//@Entity
//@Getter
//@NoArgsConstructor(access = AccessLevel.PROTECTED)
//@Table(name = "tbl_notification")
//public class Notification {
//
//    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "notification_id")
//    private Long id;
//
//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "member_id")
//    private Member member;
//
//    private String token;
//
//    @Builder
//    public Notification(String token) {
//        this.token = token;
//    }
//
//    public void confirmUser(Member member) {
//        this.member = member;
//    }
//}