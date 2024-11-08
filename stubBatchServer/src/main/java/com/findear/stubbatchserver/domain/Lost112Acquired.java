package com.findear.stubbatchserver.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "tbl_lost112_acquired")
public class Lost112Acquired {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lost112_acquired_id")
    private Long id;

    @Column(nullable = false, length = 45)
    private String atcId;


    private String depPlace;

    private String addr;

    private String fdFilePathImg;

    @Column(length = 45)
    private String fdPrdtNm;

    @Column(nullable = false, length = 45)
    private String fdSbjt;

    @Column(length = 45)
    private String clrNm;

    private LocalDate fdYmd;

    @Column(length = 45)
    private String prdtClNm;

    @Column(length = 45)
    private String mainPrdtClNm;

    @Column(length = 45)
    private String subPrdtClNm;
}
