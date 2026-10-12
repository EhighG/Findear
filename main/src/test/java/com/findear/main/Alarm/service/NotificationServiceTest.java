package com.findear.main.Alarm.service;

import com.findear.main.Alarm.common.domain.Alarm;
import com.findear.main.Alarm.common.domain.Notification;
import com.findear.main.Alarm.common.exception.AlarmException;
import com.findear.main.Alarm.dto.NotificationRequestDto;
import com.findear.main.Alarm.dto.SaveNotificationReqDto;
import com.findear.main.Alarm.push.PushMessage;
import com.findear.main.Alarm.push.PushRequestedEvent;
import com.findear.main.Alarm.repository.AlarmRepository;
import com.findear.main.Alarm.repository.NotificationRepository;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.common.domain.Role;
import com.findear.main.member.query.repository.MemberQueryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 알림 저장(DB)과 푸시 발송 이벤트 발행 검증. DB·Firebase 없이 mock만 쓴다. 발송 시점은 PushDispatchTransactionTest. */
class NotificationServiceTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final MemberQueryRepository memberQueryRepository = mock(MemberQueryRepository.class);
    private final AlarmRepository alarmRepository = mock(AlarmRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private Member member;
    private final NotificationRequestDto request = NotificationRequestDto.builder()
            .title("쪽지 도착").message("쪽지가 도착했습니다.").type("message").memberId(1L).build();

    @BeforeEach
    void setup() {
        member = Member.builder().naverUid("uid").phoneNumber("010-0000-0000").role(Role.NORMAL).build();
        when(memberQueryRepository.findById(1L)).thenReturn(Optional.of(member));
    }

    private NotificationService service() {
        return new NotificationService(notificationRepository, memberQueryRepository, alarmRepository, eventPublisher,
                mock(EntityManager.class));
    }

    private Notification tokenOf(String token) {
        Notification notification = Notification.builder().token(token).build();
        notification.confirmUser(member);
        return notification;
    }

    @DisplayName("토큰이 있으면 Alarm을 저장하고 title, 메시지:타입 본문의 발송 이벤트를 발행한다")
    @Test
    void savesAlarmAndPublishesEvent() {
        when(notificationRepository.findByMember(member)).thenReturn(tokenOf("tok"));

        service().sendNotification(request);

        ArgumentCaptor<Alarm> alarm = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmRepository).save(alarm.capture());
        assertThat(alarm.getValue().getContent()).isEqualTo("쪽지가 도착했습니다.");
        assertThat(alarm.getValue().getMember()).isSameAs(member);

        ArgumentCaptor<PushRequestedEvent> event = ArgumentCaptor.forClass(PushRequestedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().message())
                .isEqualTo(new PushMessage(1L, "tok", "쪽지 도착", "쪽지가 도착했습니다.:message"));
        verify(notificationRepository, never()).delete(any());
    }

    @DisplayName("토큰이 없으면 Alarm은 저장하고 이벤트는 발행하지 않는다")
    @Test
    void noTokenNoEvent() {
        when(notificationRepository.findByMember(member)).thenReturn(null);

        service().sendNotification(request);

        verify(alarmRepository).save(any(Alarm.class));
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @DisplayName("토큰 값이 비어 있으면 이벤트는 발행하지 않는다")
    @Test
    void blankTokenNoEvent() {
        when(notificationRepository.findByMember(member)).thenReturn(tokenOf(" "));

        service().sendNotification(request);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @DisplayName("알림을 받을 회원이 없으면 기존처럼 AlarmException이고 이벤트는 없다")
    @Test
    void memberNotFoundThrows() {
        when(memberQueryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().sendNotification(request))
                .isInstanceOf(AlarmException.class)
                .hasMessage("해당 유저가 존재하지 않습니다.");
        verify(alarmRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @DisplayName("deleteInvalidToken: 저장된 토큰이 같으면 삭제, 그 사이 새 토큰이 등록됐으면 삭제하지 않는다")
    @Test
    void deleteInvalidTokenOnlyWhenSame() {
        Notification stored = tokenOf("stale");
        when(notificationRepository.findByMemberId(1L)).thenReturn(stored);

        service().deleteInvalidToken(1L, "fresh-other");
        verify(notificationRepository, never()).delete(any());

        service().deleteInvalidToken(1L, "stale");
        verify(notificationRepository).delete(stored);
    }

    @DisplayName("deleteInvalidToken: 저장된 토큰이 없으면 아무것도 하지 않는다")
    @Test
    void deleteInvalidTokenWhenNoneStored() {
        when(notificationRepository.findByMemberId(1L)).thenReturn(null);

        service().deleteInvalidToken(1L, "stale");

        verify(notificationRepository, never()).delete(any());
    }

    @DisplayName("sendNotification: 저장소의 예상 밖 예외는 AlarmException(400)으로 바뀌지 않고 그대로 전파된다")
    @Test
    void sendNotificationPropagatesUnexpected() {
        RuntimeException dbDown = new DataAccessResourceFailureException("db down");
        when(alarmRepository.save(any(Alarm.class))).thenThrow(dbDown);

        assertThatThrownBy(() -> service().sendNotification(request)).isSameAs(dbDown);
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    private static SaveNotificationReqDto saveRequest(Long memberId) {
        SaveNotificationReqDto req = new SaveNotificationReqDto();
        req.setMemberId(memberId);
        req.setToken("tok");
        return req;
    }

    @DisplayName("saveNotification: 회원이 없으면 AlarmException(400), 저장소의 예상 밖 예외는 그대로 전파된다")
    @Test
    void saveNotification() {
        when(memberQueryRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().saveNotification(saveRequest(404L)))
                .isInstanceOf(AlarmException.class)
                .hasMessage("해당 유저가 존재하지 않습니다.");

        RuntimeException dbDown = new DataAccessResourceFailureException("db down");
        when(notificationRepository.save(any(Notification.class))).thenThrow(dbDown);
        assertThatThrownBy(() -> service().saveNotification(saveRequest(1L))).isSameAs(dbDown);
    }

    @DisplayName("saveNotification: token이 255자를 넘으면 AlarmException(400)이고 기존 토큰 삭제도 저장도 없다")
    @Test
    void saveNotificationTokenTooLong() {
        when(notificationRepository.findByMember(member)).thenReturn(tokenOf("old"));
        SaveNotificationReqDto req = saveRequest(1L);
        req.setToken("a".repeat(256));

        assertThatThrownBy(() -> service().saveNotification(req))
                .isInstanceOf(AlarmException.class)
                .hasMessage("토큰은 255자 이하여야 합니다.");
        verify(notificationRepository, never()).delete(any());
        verify(notificationRepository, never()).save(any());
    }

    @DisplayName("saveNotification: token이 정확히 255자면 기존 토큰을 지우고 저장한다")
    @Test
    void saveNotificationTokenAtLimit() {
        Notification old = tokenOf("old");
        when(notificationRepository.findByMember(member)).thenReturn(old);
        SaveNotificationReqDto req = saveRequest(1L);
        req.setToken("a".repeat(255));

        service().saveNotification(req);

        verify(notificationRepository).delete(old);
        verify(notificationRepository).save(any(Notification.class));
    }

    @DisplayName("deleteNotification: 회원이 없으면 AlarmException(400), 저장소의 예상 밖 예외는 그대로 전파된다")
    @Test
    void deleteNotification() {
        when(memberQueryRepository.findById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().deleteNotification(404L))
                .isInstanceOf(AlarmException.class)
                .hasMessage("해당 유저가 존재하지 않습니다.");

        Notification stored = tokenOf("tok");
        when(notificationRepository.findByMemberId(any())).thenReturn(stored);
        RuntimeException dbDown = new DataAccessResourceFailureException("db down");
        org.mockito.Mockito.doThrow(dbDown).when(notificationRepository).delete(stored);
        assertThatThrownBy(() -> service().deleteNotification(1L)).isSameAs(dbDown);
    }
}
