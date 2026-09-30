//package com.findear.batch.alarm.service;
//
//import com.findear.batch.alarm.domain.Notification;
//import com.findear.batch.alarm.exception.AlarmException;
//import com.findear.batch.ours.domain.Member;
//import com.findear.batch.ours.repository.MemberRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class NotificationService {
//
//    private final NotificationRepository notificationRepository;
//    private final MemberRepository memberRepository;
//
//    @Transactional
//    public void saveNotification(String token) {
//        Member member = memberRepository.findByEmail(SecurityProvider.getLoginUserEmail())
//                .orElseThrow(() -> new AlarmException(ErrorCode.RETRY_LOGIN));
//
//        Notification notification = Notification.builder()
//                .token(token)
//                .build();
//
//        notification.confirmUser(member);
//        notificationRepository.save(notification);
//    }
//}