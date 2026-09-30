package com.findear.batch.ours.repository;

import com.findear.batch.ours.domain.LostBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LostBoardRepository extends JpaRepository<LostBoard, Long> {

    @Query("select lb from LostBoard lb join fetch lb.board where lb.board.status = 'ONGOING'")
    List<LostBoard> findAllWithBoardByStatusOngoing();

    @Query("select lb from LostBoard lb join fetch lb.board where lb.board.member.id = :memberId")
    List<LostBoard> findAllWithBoardByMemberId(Long memberId);
}
