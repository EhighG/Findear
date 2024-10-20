package com.findear.stubbatchserver.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "tbl_findear_matching")
public class FindearMatching {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "findear_matching_id")
    private Long id;

    @Column(nullable = false)
    private Long lostBoardId;

    @Column(nullable = false)
    private Long acquiredBoardId;

//    @Column(nullable = false) // primitive type 컬럼엔 not null이 자동으로 붙는다.
    private float similarityRate;

    @Column(nullable = false)
    private LocalDate matchedAt;
}
