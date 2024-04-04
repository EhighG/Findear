package com.findear.main.board.command.repository;

import com.findear.main.board.common.domain.AcquiredBoard;
import com.findear.main.board.common.domain.ReturnLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface ReturnLogRepository extends JpaRepository<ReturnLog, Long> {

    Optional<ReturnLog> findFirstByAcquiredBoardAndCancelAtIsNullOrderByIdDesc(AcquiredBoard acquiredBoard);

    @Query("select count(*) from ReturnLog rl where rl.cancelAt is null and function('DATE', rl.returnedAt) = function('DATE', :date) ")
    Long countReturn(String date);
}
