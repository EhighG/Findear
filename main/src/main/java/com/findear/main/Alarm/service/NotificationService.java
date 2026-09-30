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
import com.findear.main.member.query.repository.MemberQueryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final AlarmRepository alarmRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final EntityManager em;

    @Transactional
    public void saveNotification(SaveNotificationReqDto saveNotificationReqDto) {

        try {
            Member findMember = memberQueryRepository.findById(saveNotificationReqDto.getMemberId())
                    .orElseThrow(() -> new AlarmException("해당 유저가 존재하지 않습니다."));

            Notification findNotification = notificationRepository.findByMember(findMember);

            if (findNotification != null) {
                notificationRepository.delete(findNotification);
                log.info("토큰 삭제");

                em.flush();
            }

            Notification newNotification = Notification.builder()
                    .token(saveNotificationReqDto.getToken())
                    .build();

            log.info("FCM 토큰 등록: memberId={}", findMember.getId());
            newNotification.confirmUser(findMember);

            notificationRepository.save(newNotification);

        } catch (Exception e) {
            throw new AlarmException(e.getMessage());
        }

    }

    /**
     * 알림(tbl_alarm)을 저장하고, FCM 토큰이 있으면 푸시 발송 이벤트를 발행한다.
     * 알림은 호출한 트랜잭션 안에서 저장하고, 실제 발송은 그 트랜잭션이 커밋된 뒤 PushDispatchListener가 한다
     * (트랜잭션이 없으면 바로). 알림 저장 실패(회원 없음 등)는 AlarmException으로 알리지만, 푸시 발송 실패는
     * 호출한 흐름을 깨지 않는다 (fcm.enabled=false면 발송 자체를 건너뜀).
     */
    public void sendNotification(NotificationRequestDto req) {

        log.info("알림 요청: 제목={}, 메시지={}, 타입={}, 대상 memberId={}",
                req.getTitle(), req.getMessage(), req.getType(), req.getMemberId());

        Notification notification;
        try {
            Member findMember = memberQueryRepository.findById(req.getMemberId())
                    .orElseThrow(() -> new AlarmException("해당 유저가 존재하지 않습니다."));

            Alarm alarm = Alarm.builder()
                    .generatedAt(LocalDateTime.now().toString())
                    .author("알림")
                    .readYn(false)
                    .content(req.getMessage())
                    .member(findMember).build();

            alarmRepository.save(alarm);

            notification = notificationRepository.findByMember(findMember);
        } catch (Exception e) {
            throw new AlarmException(e.getMessage());
        }

        if (notification == null || notification.getToken() == null || notification.getToken().isBlank()) {
            log.debug("FCM 토큰이 없어 푸시를 보내지 않음: memberId={}", req.getMemberId());
            return;
        }

        eventPublisher.publishEvent(new PushRequestedEvent(new PushMessage(
                req.getMemberId(),
                notification.getToken(),
                req.getTitle(),
                req.getMessage() + ":" + req.getType())));
    }

    /**
     * FCM이 UNREGISTERED로 알려 준 토큰을 지운다. 커밋 후 콜백에서 불리므로 새 트랜잭션(REQUIRES_NEW)에서 실행한다.
     * 그 사이 회원이 새 토큰을 등록했으면(저장된 값이 다르면) 지우지 않는다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteInvalidToken(Long memberId, String invalidToken) {
        Notification notification = notificationRepository.findByMemberId(memberId);
        if (notification != null && invalidToken != null && invalidToken.equals(notification.getToken())) {
            notificationRepository.delete(notification);
            log.info("유효하지 않은 FCM 토큰 삭제: memberId={}", memberId);
        }
    }

    public void deleteNotification(Long memberId) {

        try {

            Member findMember = memberQueryRepository.findById(memberId)
                    .orElseThrow(() -> new AlarmException("해당 유저가 존재하지 않습니다."));

            Notification notification = notificationRepository.findByMemberId(findMember.getId());

            if(notification != null) {
                notificationRepository.delete(notification);
            }

        } catch (Exception e) {
            throw new AlarmException(e.getMessage());
        }
    }
}