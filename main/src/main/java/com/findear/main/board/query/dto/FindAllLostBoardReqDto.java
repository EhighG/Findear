package com.findear.main.board.query.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@NoArgsConstructor
@Getter @Setter
public class FindAllLostBoardReqDto {
    private int pageNo = 1;
    private int size = 10;
    private boolean desc = true;
    private String category;
    private Long memberId;
    private LocalDate sDate;
    private LocalDate eDate;
    private String keyword;
    private String sortBy;
}
