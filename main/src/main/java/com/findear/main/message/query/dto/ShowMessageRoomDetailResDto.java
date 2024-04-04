package com.findear.main.message.query.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
public class ShowMessageRoomDetailResDto {

    private ShowMessageRoomDetailBoardDto board;

    private List<MessageDto> message;

    private String enquirerTelNum;

}
