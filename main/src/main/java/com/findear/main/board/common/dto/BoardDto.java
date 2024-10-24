package com.findear.main.board.common.dto;

import com.findear.main.board.common.domain.*;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.dto.MemberDto;
import com.findear.main.message.common.domain.MessageRoom;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoardDto {

    private Long id;

    private Boolean isLost;

    private MemberDto member;

    private List<MessageRoom> messageRoomList = new ArrayList<>();

    private List<Scrap> scrapList = new ArrayList<>();

    private List<ImgFile> imgFileList = new ArrayList<>();

    private String color;

    @OneToOne(mappedBy = "board", fetch = FetchType.LAZY)
    private AcquiredBoard acquiredBoard;

    @OneToOne(mappedBy = "board", fetch = FetchType.LAZY)
    private LostBoard lostBoard;

    private String productName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "status")
    @Enumerated(value = EnumType.STRING)
    private BoardStatus status;

    private Boolean deleteYn;

    private String registeredAt;

    private String thumbnailUrl;

    private String categoryName;

    public Board toEntity() {
        return Board.builder()
                .id(id)
                .color(color)
                .member(member != null ? member.toEntity() : null)
                .productName(productName)
                .thumbnailUrl(thumbnailUrl)
                .categoryName(categoryName)
                .isLost(isLost)
                .aiDescription(description)
                .deleteYn(deleteYn)
                .build();
    }

    public static BoardDto of(Board board) {
        Member dbMember = board.getMember();
        return BoardDto.builder()
                .id(board.getId())
                .color(board.getColor())
                .member(MemberDto.of(dbMember))
                .productName(board.getProductName())
                .thumbnailUrl(board.getThumbnailUrl())
                .categoryName(board.getCategoryName())
                .isLost(board.getIsLost())
                .description(board.getAiDescription())
                .build();
    }
}
