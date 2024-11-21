package com.findear.main.board.query.dto;

import com.findear.main.board.common.domain.Board;
import com.findear.main.board.common.domain.LostBoard;
import com.findear.main.member.command.dto.BriefMemberDto;
import com.findear.main.member.common.domain.Member;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
@NoArgsConstructor
public class LostBoardListResDto {
    private Long lostBoardId;
    private Long boardId;
    private String productName;
    private Boolean isLost;
    private String category;
    private String thumbnailUrl;
    private LocalDate lostAt;
    private String suspiciousPlace;
    private BriefMemberDto writer;

    @Builder
    public LostBoardListResDto(Long lostBoardId, Long boardId, String productName, String category, String thumbnailUrl, LocalDate lostAt, BriefMemberDto writer,
                               String suspiciousPlace) {
        this.lostBoardId = lostBoardId;
        this.boardId = boardId;
        this.productName = productName;
        this.isLost = true;
        this.category = category;
        this.thumbnailUrl = thumbnailUrl;
        this.lostAt = lostAt;
        this.suspiciousPlace = suspiciousPlace;
        this.writer = writer;
    }

    public LostBoardListResDto(Long lostBoardId, Long boardId, String productName, String category, String thumbnailUrl, LocalDate lostAt,
            Long writerId, String writerPhoneNumber, String suspiciousPlace) {

        this(lostBoardId, boardId, productName, category, thumbnailUrl, lostAt,
                new BriefMemberDto(writerId, writerPhoneNumber), suspiciousPlace);
    }

    public static LostBoardListResDto of(LostBoard lostBoard) {
        Board board = lostBoard.getBoard();
        Member writer = board.getMember();

        return LostBoardListResDto.builder()
                .lostBoardId(lostBoard.getId())
                .boardId(board.getId())
                .productName(board.getProductName())
                .category(board.getCategoryName())
                .thumbnailUrl(board.getThumbnailUrl())
                .lostAt(lostBoard.getLostAt())
                .suspiciousPlace(lostBoard.getSuspiciousPlace())
                .writer(new BriefMemberDto(writer.getId(), writer.getPhoneNumber()))
                .build();
    }
}
