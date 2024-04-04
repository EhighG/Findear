package com.findear.main.message.query.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class ShowMessageListResDto {

    private Long messageRoomId;

    private Long boardId;

    private String thumbnailUrl;

    private String productName;

    private String description;

    private String title;

    private String content;

    private String sendAt;

}
