package com.findear.main.board.command.repository;

import com.findear.main.board.common.domain.Lost112Scrap;
import com.findear.main.member.common.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface Lost112ScrapRepository extends JpaRepository<Lost112Scrap, Long> {

    @Query("select s from Lost112Scrap s join fetch s.member where s.member = :member")
    List<Lost112Scrap> findAllByMember(Member member);

    Optional<Lost112Scrap> findByMemberAndLost112AtcId(Member member, String lost112AtcId);
}
