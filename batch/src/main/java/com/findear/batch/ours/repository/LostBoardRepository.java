package com.findear.batch.ours.repository;

import com.findear.batch.ours.domain.LostBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LostBoardRepository extends JpaRepository<LostBoard, Long> {

    /** 정기 매칭 대상: 아직 찾지 못했고(ONGOING) 삭제되지 않은 분실물. 오래된 것부터 */
    @Query("select lb from LostBoard lb join fetch lb.board " +
            "where lb.board.status = 'ONGOING' and lb.board.deleteYn = false order by lb.id")
    List<LostBoard> findAllWithBoardByStatusOngoing();

    @Query("select lb from LostBoard lb join fetch lb.board where lb.board.member.id = :memberId")
    List<LostBoard> findAllWithBoardByMemberId(Long memberId);
}
