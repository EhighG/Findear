package com.findear.main.board.command.dto;

import com.findear.main.board.common.domain.ImgFile;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
public class ModifyLostBoardReqDto {
    private Long memberId;
    private Long boardId;
    private String color;
    private List<String> imgUrls;
    private List<ImgFile> imgFileList;
    private String category;
    private String lostAt;
    private String xpos;
    private String ypos;
    private String suspiciousPlace;
}
