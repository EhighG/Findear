package com.findear.main.board.command.dto;

import com.findear.main.board.common.domain.ImgFile;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private List<String> imgKeys;
    @JsonIgnore // 서비스가 imgKeys로 채운다. 요청 JSON으로는 받지 않는다
    private List<ImgFile> imgFileList;
    private String category;
    private String lostAt;
    private String xpos;
    private String ypos;
    private String suspiciousPlace;
}
