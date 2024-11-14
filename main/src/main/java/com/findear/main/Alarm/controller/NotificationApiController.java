package com.findear.main.Alarm.controller;

import com.findear.main.Alarm.dto.SaveNotificationReqDto;
import com.findear.main.Alarm.service.NotificationService;
import com.findear.main.common.response.SuccessResponse;
import com.findear.main.member.query.service.MemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
public class NotificationApiController {

    private final NotificationService notificationService;

    @PostMapping("/new")
    public ResponseEntity<?> saveNotification(@RequestBody SaveNotificationReqDto saveNotificationReqDto) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        saveNotificationReqDto.setMemberId(MemberQueryService.getAuthenticatedMemberId());

        notificationService.saveNotification(saveNotificationReqDto);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), " firebase 토큰이 갱신되었습니다.", null));
    }

}