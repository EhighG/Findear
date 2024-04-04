package com.findear.main.Alarm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@ToString
@AllArgsConstructor
@Data
public class NotificationBodyDto {

    private String message;

    private String type;
}
