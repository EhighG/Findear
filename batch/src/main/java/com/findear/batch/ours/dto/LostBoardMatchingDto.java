package com.findear.batch.ours.dto;

import com.findear.batch.ours.domain.LostBoard;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
@Data
public class LostBoardMatchingDto {

    private String lostBoardId;

    private String productName;

    private String color;

    private String categoryName;

    private String description;

    private String lostAt;

    private String xPos;

    private String yPos;

    /** 정기 매칭용: 분실물 엔티티(게시글이 함께 조회돼 있어야 한다)로 match 요청의 분실물 정보를 만든다. lostBoardId는 분실물 테이블의 PK다 */
    public static LostBoardMatchingDto from(LostBoard lostBoard) {

        return LostBoardMatchingDto.builder()
                .lostBoardId(lostBoard.getId().toString())
                .productName(lostBoard.getBoard().getProductName())
                .color(lostBoard.getBoard().getColor())
                .categoryName(lostBoard.getBoard().getCategoryName())
                .description(lostBoard.getBoard().getAiDescription())
                .lostAt(lostBoard.getLostAt().toString())
                .xPos(lostBoard.getXPos() == null ? null : lostBoard.getXPos().toString())
                .yPos(lostBoard.getYPos() == null ? null : lostBoard.getYPos().toString())
                .build();
    }
}
