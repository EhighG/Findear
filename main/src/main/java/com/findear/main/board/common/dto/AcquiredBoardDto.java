package com.findear.main.board.common.dto;

import com.findear.main.member.command.dto.BriefMemberDto;
import lombok.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcquiredBoardDto {
    private Long acquisitionId;
    private Long boardId;

    private BriefMemberDto member;
    private String color;
    private String productName;
    private String description;
    private String categoryName;
    private LocalDate registeredAt;
    private List<String> imgUrlList = new ArrayList<>();

    private LocalDate acquiredAt;

    private String address;

    private String name;

    private Float xPos;

    private Float yPos;

    public String getAcquiredAt() {
        return this.acquiredAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public String getRegisteredAt() {
        return this.registeredAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
