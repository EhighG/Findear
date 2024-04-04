package com.findear.main.Alarm.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class AlarmDataDto {

    private Long alarmId;

    private String author;

    private String content;

    private String generatedAt;

    private Boolean readYn;
}
