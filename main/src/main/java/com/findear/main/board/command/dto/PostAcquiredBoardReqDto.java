package com.findear.main.board.command.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
public class PostAcquiredBoardReqDto {
    private String productName;
    private List<String> imgUrls;
    private Long memberId;
}
