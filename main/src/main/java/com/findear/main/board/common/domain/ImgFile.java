package com.findear.main.board.common.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tbl_img_file")
public class ImgFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "img_file_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    private String imgUrl;

    public ImgFile(Long id, String imgUrl) {
        this.id = id;
        this.imgUrl = imgUrl;
    }

    public ImgFile(Board board, String imgUrl) {
        this.board = board;
        this.imgUrl = imgUrl;
    }
}
