package com.findear.main.message.query.controller;

import com.findear.main.common.response.SuccessResponse;
import com.findear.main.member.query.service.MemberQueryServiceImpl;
import com.findear.main.message.query.dto.ShowMessageListReqDto;
import com.findear.main.message.query.dto.ShowMessageListResDto;
import com.findear.main.message.query.dto.ShowMessageRoomDetailReqDto;
import com.findear.main.message.query.dto.ShowMessageRoomDetailResDto;
import com.findear.main.message.query.service.MessageQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/message")
@RestController
public class MessageQueryController {

    private final MessageQueryService messageQueryService;

    @GetMapping()
    public ResponseEntity<?> showMessageRoomList(ShowMessageListReqDto showMessageListReqDto) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        showMessageListReqDto.setMemberId(MemberQueryServiceImpl.getAuthenticatedMemberId());

        List<ShowMessageListResDto> result = messageQueryService.showMessageRoomList(showMessageListReqDto);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "쪽지방 리스트를 조회하였습니다.", result));
    }

    @GetMapping("/{messageRoomId}")
    public ResponseEntity<?> showMessageRoomDetail(@PathVariable Long messageRoomId) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));

        ShowMessageRoomDetailReqDto showMessageListReqDto = new ShowMessageRoomDetailReqDto();
        showMessageListReqDto.setMemberId(MemberQueryServiceImpl.getAuthenticatedMemberId());
        showMessageListReqDto.setMessageRoomId(messageRoomId);

        ShowMessageRoomDetailResDto result = messageQueryService.showMessageRoomDetail(showMessageListReqDto);

        return ResponseEntity.ok().body(new SuccessResponse(HttpStatus.OK.value(), "쪽지방 리스트를 조회하였습니다.", result));
    }
}
