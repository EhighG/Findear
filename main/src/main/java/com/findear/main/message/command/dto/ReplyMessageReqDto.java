package com.findear.main.message.command.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ReplyMessageReqDto {

    private Long messageRoomId;

    private Long memberId;

    private String title;

    private String content;
}
