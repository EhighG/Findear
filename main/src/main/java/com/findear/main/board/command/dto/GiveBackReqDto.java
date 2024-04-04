package com.findear.main.board.command.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
public class GiveBackReqDto {
    private Long managerId;
    private Long boardId;
    private String phoneNumber;
}
