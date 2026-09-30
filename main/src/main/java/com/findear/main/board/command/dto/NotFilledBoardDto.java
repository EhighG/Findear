package com.findear.main.board.command.dto;

import com.findear.main.board.common.domain.AcquiredBoard;
import com.findear.main.board.common.domain.Board;
import com.findear.main.storage.ImageUrls;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotFilledBoardDto {
    private String productName;
    /** match 계약(07 §5.1)의 imgUrl: 첫 이미지의 공개 URL */
    private String imgUrl;

    public static NotFilledBoardDto of(AcquiredBoard acquiredBoard) {
        Board board = acquiredBoard.getBoard();
        return new NotFilledBoardDto(board.getProductName(),
                ImageUrls.toUrl(board.getImgFileList().get(0).getImgKey()));
    }
}
