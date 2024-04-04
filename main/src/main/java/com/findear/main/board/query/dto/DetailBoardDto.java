package com.findear.main.board.query.dto;

import com.findear.main.board.common.domain.Board;
import com.findear.main.board.common.domain.BoardStatus;
import com.findear.main.board.common.domain.ImgFile;
import com.findear.main.member.command.dto.BriefMemberDto;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.dto.MemberDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetailBoardDto {
    private Long id;
    private Boolean isLost;
    private String productName;
    private String categoryName;
    private BriefMemberDto member;
    private List<String> imgUrls;
    private String color;
    private String registeredAt;
    private BoardStatus status;

    public static DetailBoardDto of(Board board) {
        Member dbMember = board.getMember();
        List<ImgFile> imgFiles = board.getImgFileList();
        return DetailBoardDto.builder()
                .id(board.getId())
                .isLost(board.getIsLost())
                .productName(board.getProductName())
                .categoryName(board.getCategoryName())
                .member(new BriefMemberDto(dbMember.getId(), dbMember.getPhoneNumber(), dbMember.getRole()))
                .imgUrls(imgFiles.stream().map(ImgFile::getImgUrl).toList())
                .color(board.getColor())
                .registeredAt(board.getRegisteredAt().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .status(board.getStatus())
                .build();
    }
}
