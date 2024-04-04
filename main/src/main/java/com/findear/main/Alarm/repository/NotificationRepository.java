package com.findear.main.Alarm.repository;

import com.findear.main.Alarm.common.domain.Notification;
import com.findear.main.member.common.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Notification findByMember(Member member);

    @Query("select n from Notification n where n.member.id = :memberId")
    Notification findByMemberId(Long memberId);
}