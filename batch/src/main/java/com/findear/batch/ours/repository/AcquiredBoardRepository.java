package com.findear.batch.ours.repository;

import com.findear.batch.ours.domain.AcquiredBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AcquiredBoardRepository extends JpaRepository<AcquiredBoard, Long> {

    @Query("select ab from AcquiredBoard ab join fetch ab.board " +
            "where ab.board.categoryName = :categoryName and ab.board.registeredAt >= :lostAt")
    List<AcquiredBoard> findAllWithBoardByCategoryAndAfterLostAt(String categoryName, LocalDateTime lostAt);
}
