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

    /** 스토리지 object key (images/{yyyy}/{MM}/{uuid}.{ext}). 응답 URL은 ImageUrls가 조립한다 */
    private String imgKey;

    public ImgFile(Long id, String imgKey) {
        this.id = id;
        this.imgKey = imgKey;
    }

    public ImgFile(Board board, String imgKey) {
        this.board = board;
        this.imgKey = imgKey;
    }
}
