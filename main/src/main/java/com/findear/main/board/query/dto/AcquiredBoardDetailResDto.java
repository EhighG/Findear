package com.findear.main.board.query.dto;

import com.findear.main.board.common.domain.AcquiredBoard;
import com.findear.main.board.common.domain.Board;
import com.findear.main.board.common.domain.ImgFile;
import com.findear.main.member.command.dto.BriefMemberDto;
import com.findear.main.member.common.domain.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class AcquiredBoardDetailResDto {
    private Long id;
    private DetailBoardDto board;
    private String address;
    private String agencyName;
    private Float xPos;
    private Float yPos;
    private String acquiredAt;

    public static AcquiredBoardDetailResDto of(AcquiredBoard acquiredBoard) {
        Board board = acquiredBoard.getBoard();
        Member dbMember = board.getMember();
        List<ImgFile> imgFiles = board.getImgFileList();

        return AcquiredBoardDetailResDto.builder()
                .id(acquiredBoard.getId())
                .board(DetailBoardDto.builder()
                        .id(board.getId())
                        .productName(board.getProductName())
                        .categoryName(board.getCategoryName())
                        .isLost(false)
                        .member(new BriefMemberDto(dbMember.getId(), dbMember.getPhoneNumber(), dbMember.getRole()))
                        .imgUrls(imgFiles.stream().map(ImgFile::getImgUrl).toList())
                        .color(board.getColor())
                        .registeredAt(board.getRegisteredAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
                        .status(board.getStatus())
                        .build())
                .address(acquiredBoard.getAddress())
                .agencyName(acquiredBoard.getName())
                .xPos(acquiredBoard.getXPos())
                .yPos(acquiredBoard.getYPos())
                .acquiredAt(acquiredBoard.getAcquiredAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .build();
    }
}
