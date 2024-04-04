package com.findear.main.Alarm.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Repository
public class EmitterRepository {

    // 동시성 문제를 위해 ConcurrentHashMap 사용
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void save(Long id, SseEmitter emitter) {

        emitters.put(id, emitter);
    }

    public void deleteById(Long roomUserNo) {

        emitters.remove(roomUserNo);
    }

    public SseEmitter get(Long roomUserNo) {

        return emitters.get(roomUserNo);
    }

}



