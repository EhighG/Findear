package com.findear.main.Alarm.controller;

import com.findear.main.Alarm.dto.AlarmDataDto;
import com.findear.main.Alarm.dto.ShowAlarmDto;
import com.findear.main.Alarm.service.AlarmService;
import com.findear.main.Alarm.service.EmitterService;
import com.findear.main.common.response.SuccessResponse;
import com.findear.main.member.query.service.MemberQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/alarm")
@RestController
public class AlarmController {

    private final EmitterService emitterService;
    private final AlarmService alarmService;

    // 구독은 본인 알림만 (K-07). 경로 모양은 그대로 두고, 인증된 회원과 경로의 memberId가 다르면 403.
    // produces를 두지 않는다: WebConfig가 Accept가 없거나 */*이면 JSON을 기본으로 삼아서, produces가 있으면 curl 같은
    // 클라이언트는 406이 된다. SseEmitter는 응답 Content-Type을 text/event-stream으로 직접 지정한다.
    @GetMapping("/subscribe/{memberId}")
    public SseEmitter subscribe(@PathVariable Long memberId, @AuthenticationPrincipal Long requestMemberId) {

        if (!memberId.equals(requestMemberId)) {
            throw new AuthorizationServiceException("본인의 알림만 구독할 수 있습니다.");
        }

        SseEmitter result = emitterService.subscribe(memberId);

        return result;
    }

    @GetMapping("/alarm-list")
    public ResponseEntity<?> showAlarmList() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        Long memberId = MemberQueryService.getAuthenticatedMemberId();

        List<AlarmDataDto> result = alarmService.showAlarmList(memberId);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "알림 리스트를 조회하였습니다.", result));
    }

    @GetMapping("/{alarmId}")
    public ResponseEntity<?> showAlarm(@PathVariable Long alarmId) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        Long memberId = MemberQueryService.getAuthenticatedMemberId();
        ShowAlarmDto showAlarmDto = ShowAlarmDto.builder().memberId(memberId).alarmId(alarmId).build();

        AlarmDataDto result = alarmService.showAlarm(showAlarmDto);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "알림을 조회하였습니다.", result));
    }

}
