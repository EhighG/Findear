package com.findear.main.Alarm.service;

import com.findear.main.Alarm.common.domain.Alarm;
import com.findear.main.Alarm.common.domain.Notification;
import com.findear.main.Alarm.common.exception.AlarmException;
import com.findear.main.Alarm.dto.NotificationRequestDto;
import com.findear.main.Alarm.push.NoopPushSender;
import com.findear.main.Alarm.push.PushMessage;
import com.findear.main.Alarm.push.PushResult;
import com.findear.main.Alarm.push.PushSender;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 알림 저장(DB)과 푸시 발송 분리 검증. DB·Firebase 없이 mock만 쓴다. */
class NotificationServiceTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final MemberQueryRepository memberQueryRepository = mock(MemberQueryRepository.class);
    private final AlarmRepository alarmRepository = mock(AlarmRepository.class);
    private final PushSender pushSender = mock(PushSender.class);

    private Member member;
    private final NotificationRequestDto request = NotificationRequestDto.builder()
            .title("쪽지 도착").message("쪽지가 도착했습니다.").type("message").memberId(1L).build();

    @BeforeEach
    void setup() {
        member = Member.builder().naverUid("uid").phoneNumber("010-0000-0000").role(Role.NORMAL).build();
        when(memberQueryRepository.findById(1L)).thenReturn(Optional.of(member));
    }

    private NotificationService service(PushSender sender) {
        return new NotificationService(notificationRepository, memberQueryRepository, alarmRepository, sender,
                mock(EntityManager.class));
    }

    private Notification tokenOf(String token) {
        Notification notification = Notification.builder().token(token).build();
        notification.confirmUser(member);
        return notification;
    }

    @DisplayName("FCM 비활성(Noop)이면 토큰이 있어도 예외 없이 Alarm만 저장되고 토큰은 그대로다")
    @Test
    void noopSenderStoresAlarmOnly() {
        when(notificationRepository.findByMember(member)).thenReturn(tokenOf("tok"));

        service(new NoopPushSender()).sendNotification(request);

        ArgumentCaptor<Alarm> captor = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("쪽지가 도착했습니다.");
        assertThat(captor.getValue().getMember()).isSameAs(member);
        verify(notificationRepository, never()).delete(any());
    }

    @DisplayName("토큰이 없으면 Alarm은 저장하고 발송은 호출하지 않는다")
    @Test
    void noTokenNoSend() {
        when(notificationRepository.findByMember(member)).thenReturn(null);

        service(pushSender).sendNotification(request);

        verify(alarmRepository).save(any(Alarm.class));
        verify(pushSender, never()).send(any());
    }

    @DisplayName("토큰이 있으면 title과 메시지:타입 본문으로 발송한다")
    @Test
    void sendsWithToken() {
        when(notificationRepository.findByMember(member)).thenReturn(tokenOf("tok"));
        when(pushSender.send(any())).thenReturn(PushResult.SENT);

        service(pushSender).sendNotification(request);

        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        verify(pushSender).send(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new PushMessage(1L, "tok", "쪽지 도착", "쪽지가 도착했습니다.:message"));
        verify(notificationRepository, never()).delete(any());
    }

    @DisplayName("TOKEN_INVALID(UNREGISTERED)면 저장된 토큰을 삭제하고 예외는 없다")
    @Test
    void invalidTokenIsDeleted() {
        Notification notification = tokenOf("stale");
        when(notificationRepository.findByMember(member)).thenReturn(notification);
        when(pushSender.send(any())).thenReturn(PushResult.TOKEN_INVALID);

        service(pushSender).sendNotification(request);

        verify(alarmRepository).save(any(Alarm.class));
        verify(notificationRepository).delete(notification);
    }

    @DisplayName("FAILED(다른 오류 코드)면 토큰은 유지하고 예외도 없다")
    @Test
    void failedKeepsToken() {
        when(notificationRepository.findByMember(member)).thenReturn(tokenOf("tok"));
        when(pushSender.send(any())).thenReturn(PushResult.FAILED);

        service(pushSender).sendNotification(request);

        verify(alarmRepository).save(any(Alarm.class));
        verify(notificationRepository, never()).delete(any());
    }

    @DisplayName("토큰 삭제가 실패해도 호출한 흐름에는 예외가 나가지 않는다")
    @Test
    void tokenDeleteFailureIsSwallowed() {
        Notification notification = tokenOf("stale");
        when(notificationRepository.findByMember(member)).thenReturn(notification);
        when(pushSender.send(any())).thenReturn(PushResult.TOKEN_INVALID);
        doThrow(new RuntimeException("db down")).when(notificationRepository).delete(notification);

        service(pushSender).sendNotification(request);
    }

    @DisplayName("알림을 받을 회원이 없으면 기존처럼 AlarmException이고 발송은 없다")
    @Test
    void memberNotFoundThrows() {
        when(memberQueryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service(pushSender).sendNotification(request))
                .isInstanceOf(AlarmException.class)
                .hasMessage("해당 유저가 존재하지 않습니다.");
        verify(alarmRepository, never()).save(any());
        verify(pushSender, never()).send(any());
    }
}
