package com.findear.main.Alarm.service;

import com.findear.main.Alarm.common.domain.Alarm;
import com.findear.main.Alarm.common.exception.AlarmException;
import com.findear.main.Alarm.dto.AlarmDataDto;
import com.findear.main.Alarm.repository.AlarmRepository;
import com.findear.main.Alarm.repository.EmitterRepository;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.query.repository.MemberQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmitterService {

    private final Map<String, SseEmitter> emitterMap = new ConcurrentHashMap<>();
    private final EmitterRepository emitterRepository;
    private final AlarmRepository alarmRepository;
    private final MemberQueryRepository memberQueryRepository;

    private static final Long DEFAULT_TIMEOUT = 300_000L;
    private static final long RECONNECTION_TIMEOUT = 3000L;

    public SseEmitter subscribe(Long memberId) {

        SseEmitter emitter = createEmitter(memberId);

        sendToClient(memberId, "memberId : " + memberId + "과의 알람 연결 성공", "sse 접속 성공");

        return emitter;
    }

    public void alarmWithNoStore(Long memberId, AlarmDataDto alarmDataDto, String comment, String type) {
        try {
            sendToClient(memberId, alarmDataDto, comment, type);

        } catch (Exception e) {
            throw new AlarmException(e.getMessage());
        }

    }

    public void alarm(Long memberId, AlarmDataDto alarmDataDto, String comment, String type) {

        try {

            Member findMember = memberQueryRepository.findById(memberId)
                            .orElseThrow(() -> new AlarmException("해당 유저가 존재하지 않습니다."));

            sendToClient(memberId, alarmDataDto, comment, type);

            Alarm alarm = Alarm.builder()
                    .generatedAt(LocalDateTime.now().toString())
                    .author(alarmDataDto.getAuthor())
                    .readYn(false)
                    .content(alarmDataDto.getContent())
                    .member(findMember).build();

            alarmRepository.save(alarm);

        } catch (Exception e) {
            throw new AlarmException(e.getMessage());
        }
    }

    private void sendToClient(Long memberId, Object data, String comment) {

        SseEmitter emitter = emitterRepository.get(memberId);
        System.out.println("sendToClient 입성");
        if(emitter != null) {
            try {
                System.out.println("sendToClient 발송");
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(memberId))
                        .name("sse")
                        .reconnectTime(RECONNECTION_TIMEOUT)
                        .data(data)
                        .comment(comment));

            } catch (IOException e) {
                emitterRepository.deleteById(memberId);
                emitter.completeWithError(e);
            }
        }

    }

    private <T> void sendToClient(Long memberId, T data, String comment, String type) {
        SseEmitter emitter = emitterRepository.get(memberId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(memberId))
                        .name(type)
                        .reconnectTime(RECONNECTION_TIMEOUT)
                        .data(data)
                        .comment(comment));
            } catch (IOException e) {
                emitterRepository.deleteById(memberId);
                emitter.completeWithError(e);
            }
        }
    }

    private SseEmitter createEmitter(Long memberId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitterRepository.save(memberId, emitter);

        emitter.onCompletion(() -> emitterRepository.deleteById(memberId));
        emitter.onTimeout(() -> emitterRepository.deleteById(memberId));

        return emitter;
    }

}

