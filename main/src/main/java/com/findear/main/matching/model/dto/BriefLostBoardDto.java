package com.findear.main.matching.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class BriefLostBoardDto {
    private Long boardId;
    private Long lostBoardId;
    private String productName;
}
