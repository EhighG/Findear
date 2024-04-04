package com.findear.main.matching.model.dto;

import com.findear.main.board.query.dto.LostBoardDetailResDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor
public class LostBoardMatchingDto {
    private Long boardId;
    private String productName;

    public static LostBoardMatchingDto fromDetailResDto(LostBoardDetailResDto lostBoardDetailResDto) {
        return new LostBoardMatchingDto(lostBoardDetailResDto.getBoard().getId(),
                lostBoardDetailResDto.getBoard().getProductName());
    }
}
