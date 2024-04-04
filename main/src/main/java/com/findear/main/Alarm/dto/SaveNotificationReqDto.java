package com.findear.main.Alarm.dto;

import lombok.Data;

@Data
public class SaveNotificationReqDto {

    private String token;

    private Long memberId;
}
