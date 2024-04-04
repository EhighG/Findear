package com.findear.main.message.query.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ShowMessageRoomDetailBoardDto {

    private Long boardId;
    private String thumbnailUrl;
    private String productName;
    private String description;
}
