package com.findear.main.Alarm.controller;

import com.findear.main.Alarm.dto.AlarmDataDto;
import com.findear.main.Alarm.dto.NotificationRequestDto;
import com.findear.main.Alarm.service.AlarmService;
import com.findear.main.Alarm.service.EmitterService;
import com.findear.main.common.profile.LocalProfile;
import com.findear.main.common.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 알림 테스트 발송 (K-07). local 프로필에서만 등록된다 (D-26). 토큰은 local에서도 필요하다.
 */
@Profile(LocalProfile.NAME)
@RequiredArgsConstructor
@RequestMapping("/alarm")
@RestController
public class LocalAlarmController {

    private final EmitterService emitterService;
    private final AlarmService alarmService;

    @PostMapping("/send-data/{memberId}")
    public void sendDataTest(@PathVariable Long memberId, @RequestBody AlarmDataDto alarmDataDto) {

        alarmDataDto.setGeneratedAt(LocalDateTime.now().toString());
        emitterService.alarm(memberId, alarmDataDto, "알림 갔니 인성아", "message");
    }

    @PostMapping("/send-fcm/{memberId}")
    public ResponseEntity<?> sendFcmAlarm(@PathVariable Long memberId,
                                          @RequestBody NotificationRequestDto notificationRequestDto) {

        notificationRequestDto.setMemberId(memberId);
        alarmService.sendFcmAlarm(notificationRequestDto);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "FCM 알람을 보냈습니다.", null));
    }
}
