package com.findear.main.Alarm.repository;

import com.findear.main.Alarm.common.domain.Alarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {

    @Query("select a from Alarm a where a.member.id = :memberId order by a.id desc")
    List<Alarm> findAllByMemberId(Long memberId);

    @Query("select a from Alarm a where a.id = :alarmId and a.member.id = :memberId")
    Alarm findByAlarmIdAndMemberId(Long alarmId, Long memberId);

}

