package com.findear.main.matching.model.dto;

import com.findear.main.board.common.domain.BoardStatus;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BriefAcquiredBoardDto {
    private Long boardId;
    private String productName;
    private String category;
    private String acquiredAt;
    private String agencyName;
    private String agencyAddress;
    private String thumbnailUrl;
    private BoardStatus status;
}
