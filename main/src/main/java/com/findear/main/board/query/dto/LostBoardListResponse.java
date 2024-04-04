package com.findear.main.board.query.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@AllArgsConstructor
public class LostBoardListResponse {
    private List<LostBoardListResDto> boardList;
    private Integer totalPageNum;
}
