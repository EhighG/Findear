package com.findear.main.message.command.dto;

import lombok.Data;

@Data
public class SendMessageReqDto {

    private Long boardId;

    private Long memberId;

    private String title;

    private String content;
}
