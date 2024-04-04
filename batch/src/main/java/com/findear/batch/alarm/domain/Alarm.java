package com.findear.batch.alarm.domain;

import com.findear.batch.ours.domain.Member;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tbl_alarm")
public class Alarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alarm_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private String author;

    private String content;

    private LocalDateTime generatedAt;

    private Boolean readYn;

    @Builder
    public Alarm(Member member, String author, String content, LocalDateTime generatedAt, Boolean readYn) {

        this.member = member;
        this.author = author;
        this.content = content;
        this.generatedAt = generatedAt;
        this.readYn = readYn;
    }

    public void readAlarm() {
        this.readYn = true;
    }


}