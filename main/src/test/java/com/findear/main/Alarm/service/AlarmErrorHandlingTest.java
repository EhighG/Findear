package com.findear.main.Alarm.service;

import com.findear.main.Alarm.common.domain.Alarm;
import com.findear.main.Alarm.common.exception.AlarmException;
import com.findear.main.Alarm.dto.AlarmDataDto;
import com.findear.main.Alarm.dto.ShowAlarmDto;
import com.findear.main.Alarm.repository.AlarmRepository;
import com.findear.main.Alarm.repository.EmitterRepository;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.domain.Role;
import com.findear.main.member.query.repository.MemberQueryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * D-51: 의도한 도메인 오류(AlarmException)만 400이고, 예상하지 못한 예외는 감싸지 않아 advice가 500으로 처리한다.
 * 예전에는 catch (Exception)이 모든 예외를 AlarmException(400)으로 바꿨다.
 */
class AlarmErrorHandlingTest {

    private AlarmRepository alarmRepository;
    private MemberQueryRepository memberQueryRepository;
    private AlarmService alarmService;
    private EmitterService emitterService;

    @BeforeEach
    void setUp() {
        alarmRepository = mock(AlarmRepository.class);
        memberQueryRepository = mock(MemberQueryRepository.class);
        alarmService = new AlarmService(alarmRepository, mock(NotificationService.class));
        emitterService = new EmitterService(mock(EmitterRepository.class), alarmRepository, memberQueryRepository);
    }

    private static ShowAlarmDto show(long memberId, long alarmId) {
        return ShowAlarmDto.builder().memberId(memberId).alarmId(alarmId).build();
    }

    @Test
    @DisplayName("showAlarmList: 저장소의 예상 밖 예외는 AlarmException으로 바뀌지 않고 그대로 전파된다")
    void showAlarmListPropagatesUnexpected() {
        RuntimeException dbDown = new DataAccessResourceFailureException("db down");
        when(alarmRepository.findAllByMemberId(1L)).thenThrow(dbDown);

        assertThatThrownBy(() -> alarmService.showAlarmList(1L)).isSameAs(dbDown);
    }

    @Test
    @DisplayName("showAlarm: 저장소의 예상 밖 예외는 AlarmException으로 바뀌지 않고 그대로 전파된다")
    void showAlarmPropagatesUnexpected() {
        RuntimeException dbDown = new DataAccessResourceFailureException("db down");
        when(alarmRepository.findByAlarmIdAndMemberId(5L, 1L)).thenThrow(dbDown);

        assertThatThrownBy(() -> alarmService.showAlarm(show(1L, 5L))).isSameAs(dbDown);
    }

    @Test
    @DisplayName("showAlarm: 없는 알림(또는 남의 알림)은 NPE 500이 아니라 AlarmException(400) \"해당 알림이 없습니다.\"")
    void showAlarmMissing() {
        when(alarmRepository.findByAlarmIdAndMemberId(5L, 1L)).thenReturn(null);

        assertThatThrownBy(() -> alarmService.showAlarm(show(1L, 5L)))
                .isInstanceOf(AlarmException.class)
                .hasMessage("해당 알림이 없습니다.");
    }

    @Test
    @DisplayName("showAlarm: 본인의 알림이면 읽음 처리하고 돌려준다")
    void showAlarmMarksRead() {
        Alarm alarm = Alarm.builder().author("알림").content("내용").generatedAt("2026-10-12T10:00:00").readYn(false).build();
        when(alarmRepository.findByAlarmIdAndMemberId(5L, 1L)).thenReturn(alarm);

        AlarmDataDto result = alarmService.showAlarm(show(1L, 5L));

        assertThat(result.getReadYn()).isTrue();
        assertThat(result.getContent()).isEqualTo("내용");
    }

    @Test
    @DisplayName("EmitterService.alarm: 회원이 없으면 AlarmException(400), 저장소의 예상 밖 예외는 그대로 전파된다")
    void emitterAlarm() {
        AlarmDataDto data = AlarmDataDto.builder().author("a").content("c").build();
        when(memberQueryRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> emitterService.alarm(404L, data, "c", "message"))
                .isInstanceOf(AlarmException.class)
                .hasMessage("해당 유저가 존재하지 않습니다.");

        Member member = Member.builder().naverUid("u").phoneNumber("010-0000-0000").role(Role.NORMAL).build();
        when(memberQueryRepository.findById(1L)).thenReturn(Optional.of(member));
        RuntimeException dbDown = new DataAccessResourceFailureException("db down");
        when(alarmRepository.save(any(Alarm.class))).thenThrow(dbDown);
        assertThatThrownBy(() -> emitterService.alarm(1L, data, "c", "message")).isSameAs(dbDown);
    }

    @Test
    @DisplayName("EmitterService.alarmWithNoStore: 구독자가 없어도 아무것도 저장하지 않고 예외도 없다")
    void emitterAlarmWithNoStore() {
        AlarmDataDto data = AlarmDataDto.builder().author("a").content("c").build();

        emitterService.alarmWithNoStore(1L, data, "c", "message");

        verify(alarmRepository, never()).save(any());
    }
}
