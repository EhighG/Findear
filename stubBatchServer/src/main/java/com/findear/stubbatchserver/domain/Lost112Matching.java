package com.findear.stubbatchserver.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "tbl_lost112_matching")
public class Lost112Matching {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lost112_matching_id")
    private Long id;

    /**
     * main DB의 tbl_lost_board의 ID를 가리킴
     */
    @Column(nullable = false)
    private Long lostBoardId;

    @ManyToOne // 항상 Lost112Acquired 데이터와 같이 조회되므로, Eager fetch
    @JoinColumn(name = "acquired_board_id")
    private Lost112Acquired lost112Acquired;

//    @Column(nullable = false)
    private float similarityRate;

    @Column(nullable = false)
    private LocalDate matchedAt;
}
