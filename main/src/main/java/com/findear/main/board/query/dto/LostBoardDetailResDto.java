package com.findear.main.board.query.dto;

import com.findear.main.board.common.domain.Board;
import com.findear.main.board.common.domain.ImgFile;
import com.findear.main.board.common.domain.LostBoard;
import com.findear.main.member.command.dto.BriefMemberDto;
import com.findear.main.member.common.domain.Member;
import lombok.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LostBoardDetailResDto {
    private Long lostBoardId;
    private DetailBoardDto board;
    private String lostAt;
    private String suspiciousPlace;
    private Float xpos;
    private Float ypos;

    public static LostBoardDetailResDto of(LostBoard lostBoard) {
        Board board = lostBoard.getBoard();
        Member dbMember = board.getMember();
        List<ImgFile> imgFiles = board.getImgFileList();

        return LostBoardDetailResDto.builder()
                .lostBoardId(lostBoard.getId())
                .board(DetailBoardDto.builder()
                        .id(board.getId())
                        .productName(board.getProductName())
                        .categoryName(board.getCategoryName())
                        .isLost(true)
                        .member(new BriefMemberDto(dbMember.getId(), dbMember.getPhoneNumber(), dbMember.getRole()))
                        .imgUrls(imgFiles.stream().map(ImgFile::getImgUrl).toList())
                        .color(board.getColor())
                        .registeredAt(board.getRegisteredAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
                        .build())
                .lostAt(lostBoard.getLostAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .suspiciousPlace(lostBoard.getSuspiciousPlace())
                .xpos(lostBoard.getXPos())
                .ypos(lostBoard.getYPos())
                .build();
    }
}
