package com.findear.batch.ours.domain;

import com.findear.batch.alarm.domain.Alarm;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tbl_member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<Board> boardList = new ArrayList<>();

    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<Alarm> alarmList = new ArrayList<>();


    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String phoneNumber;

    @CreatedDate
    private LocalDateTime joinedAt;

    private LocalDateTime withdrawalAt;

    private Boolean withdrawalYn;

}
