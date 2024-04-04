package com.findear.main.board.common.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter @Setter
@NoArgsConstructor
public class LostBoardDto {
    private Long id;

    private BoardDto board;

    private String lostAt;

    private String suspiciousPlace;

    private Float xPos;

    private Float yPos;

    @Builder
    public LostBoardDto(Long id, BoardDto board, String lostAt, String suspiciousPlace, Float xPos, Float yPos) {
        this.id = id;
        this.board = board;
        this.lostAt = lostAt;
        this.suspiciousPlace = suspiciousPlace;
        this.xPos = xPos;
        this.yPos = yPos;
    }

    public LostBoard toEntity() {
        return LostBoard.builder()
                .id(id)
                .board(board.toEntity())
                .lostAt(LocalDate.parse(lostAt, DateTimeFormatter.ISO_LOCAL_DATE))
                .suspiciousPlace(suspiciousPlace)
                .xPos(xPos)
                .yPos(yPos)
                .build();
    }

//    public static LostBoardDto of(LostBoard lostBoard) {
//        Board dbBoard = lostBoard.getBoard();
//        return LostBoardDto.builder()
//                .id(lostBoard.getId())
//                .board(
//                        BoardDto.builder()
//                                .id(dbBoard.getId())
//                                .productName(dbBoard.getProductName())
//                                .color(dbBoard.getColor())
//                                .imgFileList().build()
//                )
//                .lostAt(lostBoard.getLostAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
//                .suspiciousPlace(lostBoard.getSuspiciousPlace())
//                .xPos(lostBoard.getXPos())
//                .yPos(lostBoard.getYPos())
//                .build();
//    }
}
