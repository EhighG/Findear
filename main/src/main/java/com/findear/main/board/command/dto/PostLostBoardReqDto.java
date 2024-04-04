package com.findear.main.board.command.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@ToString
@Getter @Setter
@NoArgsConstructor
public class PostLostBoardReqDto {
    private String productName;
    private Long memberId;
    private String category;
    private String color;
    private String content;
    private List<String> imgUrls;
    private String lostAt;
    private String xpos;
    private String ypos;
    private String suspiciousPlace;
}
