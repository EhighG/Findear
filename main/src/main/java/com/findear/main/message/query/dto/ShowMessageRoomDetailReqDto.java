package com.findear.main.message.query.dto;

import lombok.Data;

@Data
public class ShowMessageRoomDetailReqDto {

    private Long memberId;
    private Long messageRoomId;
}
