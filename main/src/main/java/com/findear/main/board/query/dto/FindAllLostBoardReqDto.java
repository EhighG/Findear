package com.findear.main.board.query.dto;

import lombok.*;

@NoArgsConstructor
@Getter @Setter
public class FindAllLostBoardReqDto {
    private int pageNo = 1;
    private int size = 10;
    private boolean desc = true;
    private String category;
    private Long memberId;
    private String sDate;
    private String eDate;
    private String keyword;
    private String sortBy;
}
