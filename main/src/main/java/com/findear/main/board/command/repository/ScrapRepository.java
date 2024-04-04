package com.findear.main.board.command.repository;

import com.findear.main.board.common.domain.Board;
import com.findear.main.board.common.domain.Scrap;
import com.findear.main.member.common.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    Optional<Scrap> findByMemberAndBoard(Member member, Board board);
    List<Scrap> findAllByMember(Member member);
}
