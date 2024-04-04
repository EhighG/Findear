package com.findear.main.board.command.dto;

import com.findear.main.board.common.domain.AcquiredBoard;
import com.findear.main.board.common.domain.Board;
import com.findear.main.board.common.domain.ImgFile;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class NotFilledBoardDto {
    private String productName;
//    private List<String> imgUrls;
    private String imgUrl;

    public static NotFilledBoardDto of(AcquiredBoard acquiredBoard) {
        Board board = acquiredBoard.getBoard();
//        return new NotFilledBoardDto(board.getProductName(),
//                board.getImgFileList().stream().map(ImgFile::getImgUrl).toList());
        return new NotFilledBoardDto(board.getProductName(),
                board.getImgFileList().get(0).getImgUrl());
    }
}
