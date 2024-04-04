package com.findear.main.matching.model.dto;

import com.findear.main.board.common.domain.AcquiredBoard;
import com.findear.main.board.common.domain.Board;
import com.findear.main.board.common.domain.LostBoard;
import lombok.*;

import java.time.format.DateTimeFormatter;

@Getter @Setter
@NoArgsConstructor
public class FindearMatchingResDto {
    private BriefLostBoardDto lostBoard;
    private BriefAcquiredBoardDto acquiredBoard;
    private Float similarityRate;
    private String matchedAt;

    public FindearMatchingResDto(LostBoard lostBoard, AcquiredBoard acquiredBoard, Float similarityRate, String matchedAt) {
        Board lBoard = lostBoard.getBoard();
        Board aBoard = acquiredBoard.getBoard();
        this.lostBoard = new BriefLostBoardDto(lBoard.getId(), lostBoard.getId(), lBoard.getProductName());
        this.acquiredBoard = BriefAcquiredBoardDto.builder()
                .boardId(aBoard.getId())
                .productName(aBoard.getProductName())
                .category(aBoard.getCategoryName())
                .thumbnailUrl(aBoard.getThumbnailUrl())
                .agencyName(acquiredBoard.getName())
                .agencyAddress(acquiredBoard.getAddress())
                .acquiredAt(acquiredBoard.getAcquiredAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .status(aBoard.getStatus())
                .build();
        this.similarityRate = similarityRate;
        this.matchedAt = matchedAt;
    }
}
