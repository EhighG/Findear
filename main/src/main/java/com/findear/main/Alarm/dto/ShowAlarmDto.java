package com.findear.main.Alarm.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class ShowAlarmDto {

    private Long memberId;
    private Long alarmId;

}

