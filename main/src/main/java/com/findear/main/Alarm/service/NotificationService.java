package com.findear.main.Alarm.service;

import com.findear.main.Alarm.common.domain.Alarm;
import com.findear.main.Alarm.common.domain.Notification;
import com.findear.main.Alarm.common.exception.AlarmException;
import com.findear.main.Alarm.dto.NotificationBodyDto;
import com.findear.main.Alarm.dto.NotificationRequestDto;
import com.findear.main.Alarm.dto.SaveNotificationReqDto;
import com.findear.main.Alarm.repository.AlarmRepository;
import com.findear.main.Alarm.repository.NotificationRepository;
import com.findear.main.member.common.domain.Member;
import com.findear.main.member.query.repository.MemberQueryRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.google.firebase.messaging.Message;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final AlarmRepository alarmRepository;

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

    public void sendNotification(NotificationRequestDto req) {

        try {

            log.info("제목 : " + req.getTitle());
            log.info("메시지 : " + req.getMessage());
            log.info("메시지 타입 : " + req.getType());
            log.info("보내질 사용자 식별키 : " + req.getMemberId());
            log.info("토큰 : " + getNotificationToken(req.getMemberId()));

            Member findMember = memberQueryRepository.findById(req.getMemberId())
                    .orElseThrow(() -> new AlarmException("알림을 받을 유저가 존재하지 않습니다."));

            Alarm alarm = Alarm.builder()
                    .generatedAt(LocalDateTime.now().toString())
                    .author("알림")
                    .readYn(false)
                    .content(req.getMessage())
                    .member(findMember).build();

            alarmRepository.save(alarm);

            NotificationBodyDto notificationBodyDto = new NotificationBodyDto(req.getMessage(), req.getType());

            String token = getNotificationToken(req.getMemberId());
            String response = null;

            if(token != null) {

                Message message = Message.builder()
                        .setWebpushConfig(WebpushConfig.builder()
                                .setNotification(WebpushNotification.builder()
                                        .setTitle(req.getTitle())
                                        .setBody(notificationBodyDto.getMessage() + ":" + notificationBodyDto.getType())
                                        .build())
                                .build())
                        .setToken(getNotificationToken(req.getMemberId()))
                        .build();

                response = FirebaseMessaging.getInstance().sendAsync(message).get();
            }

            log.info(">>>>Send message : " + response);
        } catch (Exception e) {
            throw new AlarmException(e.getMessage());
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