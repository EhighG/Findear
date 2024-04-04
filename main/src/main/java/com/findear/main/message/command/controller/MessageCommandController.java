package com.findear.main.message.command.controller;

import com.findear.main.common.response.SuccessResponse;
import com.findear.main.member.query.service.MemberQueryService;
import com.findear.main.message.command.dto.ReplyMessageReqDto;
import com.findear.main.message.command.dto.SendMessageReqDto;
import com.findear.main.message.command.service.MessageCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/message")
@RestController
public class MessageCommandController {

    private final MessageCommandService messageCommandService;

    @PostMapping
    public ResponseEntity<?> sendMessage(@RequestBody SendMessageReqDto sendMessageReqDto) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        sendMessageReqDto.setMemberId(MemberQueryService.getAuthenticatedMemberId());

        messageCommandService.sendMessage(sendMessageReqDto);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "쪽지를 보냈습니다.", null));

    }

    @PostMapping("/reply")
    public ResponseEntity<?> replyMessage(@RequestBody ReplyMessageReqDto replyMessageReqDto) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        replyMessageReqDto.setMemberId(MemberQueryService.getAuthenticatedMemberId());

        messageCommandService.replyMessage(replyMessageReqDto);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "답장을 보냈습니다.", null));

    }
}
