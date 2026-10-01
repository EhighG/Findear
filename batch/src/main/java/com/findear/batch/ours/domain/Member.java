package com.findear.batch.ours.domain;

import lombok.*;

import jakarta.persistence.*;
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

    @Builder.Default
    @OneToMany(mappedBy = "member", fetch = FetchType.LAZY)
    private List<Board> boardList = new ArrayList<>();

    @Column(nullable = false)
    private String phoneNumber;

    private LocalDateTime joinedAt;

    private LocalDateTime withdrawalAt;

    private Boolean withdrawalYn;

}
