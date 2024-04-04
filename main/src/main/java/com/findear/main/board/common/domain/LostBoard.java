package com.findear.main.board.common.domain;

import com.findear.main.board.command.dto.ModifyLostBoardReqDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tbl_lost_board")
public class LostBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lost_board_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    private LocalDate lostAt;

    private String suspiciousPlace;

    private Float xPos;

    private Float yPos;

    public void modify(ModifyLostBoardReqDto modifyReqDto) {
        if (modifyReqDto.getLostAt() != null) {
            this.lostAt = LocalDate.parse(modifyReqDto.getLostAt(), DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (modifyReqDto.getSuspiciousPlace() != null) {
            this.suspiciousPlace = modifyReqDto.getSuspiciousPlace();
            this.xPos = Float.parseFloat(modifyReqDto.getXpos());
            this.yPos = Float.parseFloat(modifyReqDto.getYpos());
        }
        this.board.modify(modifyReqDto.getColor(), modifyReqDto.getImgFileList(), modifyReqDto.getCategory());
    }

}
