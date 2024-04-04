package com.findear.main.board.query.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@AllArgsConstructor
public class AcquiredBoardListResponse {

    private List<AcquiredBoardListResDto> boardList;
    private Integer totalPageNum;
}
