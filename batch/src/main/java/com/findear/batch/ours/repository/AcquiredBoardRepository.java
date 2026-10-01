package com.findear.batch.ours.repository;

import com.findear.batch.ours.domain.AcquiredBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AcquiredBoardRepository extends JpaRepository<AcquiredBoard, Long> {

    /** 매칭 후보: 같은 카테고리, {@code lostAt} 이후 등록, 삭제되지 않았고 아직 반환되지 않은(ONGOING) 습득물 */
    @Query("select ab from AcquiredBoard ab join fetch ab.board " +
            "where ab.board.categoryName = :categoryName and ab.board.registeredAt >= :lostAt " +
            "and ab.board.deleteYn = false and ab.board.status = 'ONGOING'")
    List<AcquiredBoard> findAllWithBoardByCategoryAndAfterLostAt(String categoryName, LocalDateTime lostAt);
}
