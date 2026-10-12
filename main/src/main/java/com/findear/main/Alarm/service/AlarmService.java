package com.findear.main.Alarm.service;

import com.findear.main.Alarm.common.domain.Alarm;
import com.findear.main.Alarm.common.exception.AlarmException;
import com.findear.main.Alarm.dto.AlarmDataDto;
import com.findear.main.Alarm.dto.NotificationRequestDto;
import com.findear.main.Alarm.dto.ShowAlarmDto;
import com.findear.main.Alarm.repository.AlarmRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class AlarmService {

    private final AlarmRepository alarmRepository;
    private final NotificationService notificationService;

    public List<AlarmDataDto> showAlarmList(Long memberId) {

        List<Alarm> findAlarms = alarmRepository.findAllByMemberId(memberId);

        List<AlarmDataDto> results = new ArrayList<>();
        for(Alarm a : findAlarms) {

            results.add(AlarmDataDto.builder()
                    .alarmId(a.getId())
                    .author(a.getAuthor())
                    .content(a.getContent())
                    .generatedAt(a.getGeneratedAt().toString())
                    .readYn(a.getReadYn()).build());
        }

        return results;
    }

    public AlarmDataDto showAlarm(ShowAlarmDto showAlarmDto) {

        Alarm findAlarm = alarmRepository.findByAlarmIdAndMemberId(showAlarmDto.getAlarmId(), showAlarmDto.getMemberId());

        // 없는 알림과 남의 알림은 같은 답으로 한다 (알림 존재 여부를 흘리지 않는다)
        if (findAlarm == null) {
            throw new AlarmException("해당 알림이 없습니다.");
        }

        findAlarm.readAlarm();

        AlarmDataDto result = AlarmDataDto.builder()
                .alarmId(findAlarm.getId())
                .author(findAlarm.getAuthor())
                .content(findAlarm.getContent())
                .readYn(findAlarm.getReadYn())
                .generatedAt(findAlarm.getGeneratedAt().toString()).build();

        return result;
    }

    public void sendFcmAlarm(NotificationRequestDto notificationRequestDto) {

        notificationService.sendNotification(notificationRequestDto);
        log.info("fcm 알림을 보냈습니다.");
    }
}
