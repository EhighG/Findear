package com.findear.main.board.command.dto;

import com.findear.main.board.common.domain.ImgFile;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
public class ModifyAcquiredBoardReqDto {
    private Long memberId;
    private Long boardId;
    private String color;
    private List<String> imgUrls;
    private List<ImgFile> imgFileList;
    private String category;
    private String acquiredAt;
}
