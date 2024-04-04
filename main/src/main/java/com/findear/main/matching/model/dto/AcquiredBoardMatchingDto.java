package com.findear.main.matching.model.dto;

import com.findear.main.board.query.dto.AcquiredBoardDetailResDto;
import com.findear.main.board.query.dto.DetailBoardDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Builder
@AllArgsConstructor
public class AcquiredBoardMatchingDto {
    private Long boardId;
    private String productName;
    private String category;
    private String type;
    private String acquiredAt;

    public static AcquiredBoardMatchingDto fromDetailResDto(AcquiredBoardDetailResDto acquiredBoardDetailResDto, Boolean isLost112Data) {
        DetailBoardDto board = acquiredBoardDetailResDto.getBoard();
        return AcquiredBoardMatchingDto.builder()
                .boardId(board.getId())
                .productName(board.getProductName())
                .category(board.getCategoryName())
                .type(isLost112Data ? "Lost112" : "Findear")
                .acquiredAt(acquiredBoardDetailResDto.getAcquiredAt())
                .build();
    }
}
