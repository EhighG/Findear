package com.findear.main.Alarm.service;

import com.findear.main.Alarm.common.domain.Alarm;
import com.findear.main.Alarm.common.domain.Notification;
import com.findear.main.Alarm.common.exception.AlarmException;
import com.findear.main.Alarm.dto.NotificationRequestDto;
import com.findear.main.Alarm.dto.SaveNotificationReqDto;
import com.findear.main.Alarm.push.PushMessage;
import com.findear.main.Alarm.push.PushResult;
import com.findear.main.Alarm.push.PushSender;
import com.findear.main.Alarm.repository.AlarmRepository;
import com.findear.main.Alarm.repository.NotificationRepository;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.query.repository.MemberQueryRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final AlarmRepository alarmRepository;
    private final PushSender pushSender;

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

            log.info("새로운 토큰 : " + newNotification.getToken());
            newNotification.confirmUser(findMember);

            notificationRepository.save(newNotification);

        } catch (Exception e) {
            throw new AlarmException(e.getMessage());
        }

    }

    /**
     * 알림(tbl_alarm)을 저장하고, FCM 토큰이 있으면 웹푸시를 보낸다.
     * 알림 저장 실패(회원 없음 등)는 AlarmException으로 알리지만, 푸시 발송 실패는 호출한 흐름을 깨지 않도록
     * 로그만 남긴다 (fcm.enabled=false면 발송 자체를 건너뜀). 토큰이 무효(UNREGISTERED)면 토큰을 지운다.
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

        PushResult result = pushSender.send(new PushMessage(
                req.getMemberId(),
                notification.getToken(),
                req.getTitle(),
                req.getMessage() + ":" + req.getType()));

        if (result == PushResult.TOKEN_INVALID) {
            try {
                notificationRepository.delete(notification);
                log.info("유효하지 않은 FCM 토큰 삭제: memberId={}", req.getMemberId());
            } catch (Exception e) {
                log.warn("유효하지 않은 FCM 토큰 삭제 실패: memberId={}, {}", req.getMemberId(), e.toString());
            }
        }
    }

    public String getNotificationToken(Long memberId) {

        try {

            Member findMember = memberQueryRepository.findById(memberId)
                    .orElseThrow(() -> new AlarmException("해당 유저가 존재하지 않습니다."));

            Notification notification = notificationRepository.findByMember(findMember);

            if(notification == null) {
                return null;
            }
            return notification.getToken();

        } catch (Exception e) {
            throw new AlarmException(e.getMessage());
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