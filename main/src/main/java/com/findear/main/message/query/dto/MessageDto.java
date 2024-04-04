package com.findear.main.message.query.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class MessageDto {

    private Long messageId;
    private Long messageRoomId;
    private String title;
    private String content;
    private String sendAt;
    private Long senderId;
}
