package com.findear.main.board.query.dto;

import com.findear.main.board.common.domain.AcquiredBoard;
import com.findear.main.board.common.domain.Board;
import com.findear.main.board.common.domain.LostBoard;
import com.findear.main.member.command.dto.BriefMemberDto;
import com.findear.main.member.command.dto.LoginResAgencyDto;
import com.findear.main.member.common.domain.Agency;
import com.findear.main.member.common.domain.Member;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcquiredBoardListResDto {
    private Long boardId;
    private Boolean isLost;
    private String productName;
    private String category;
    private String thumbnailUrl;
    private LoginResAgencyDto agency;
    private String acquiredAt;
    private BriefMemberDto writer;

    public static AcquiredBoardListResDto of(AcquiredBoard acquiredBoard) {
        Board board = acquiredBoard.getBoard();
        Member writer = board.getMember();
        Agency dbAgency = board.getMember().getAgency();

        return AcquiredBoardListResDto.builder()
                .boardId(board.getId())
                .isLost(false)
                .productName(board.getProductName())
                .category(board.getCategoryName())
                .thumbnailUrl(board.getThumbnailUrl())
                .agency(new LoginResAgencyDto(dbAgency.getId(), dbAgency.getName(), dbAgency.getAddress()))
                .acquiredAt(acquiredBoard.getAcquiredAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .writer(new BriefMemberDto(writer.getId(), writer.getPhoneNumber()))
                .build();
    }
}
