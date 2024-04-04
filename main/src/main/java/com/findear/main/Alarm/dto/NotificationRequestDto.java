package com.findear.main.Alarm.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NotificationRequestDto {

    private String title;
    private String message;
    private String type;
    private Long memberId;

    @Builder
    public NotificationRequestDto(String title, String message, String type, Long memberId) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.memberId = memberId;
    }
}