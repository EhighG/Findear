package com.findear.main.board.command.repository;

import com.findear.main.board.common.domain.AcquiredBoard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcquiredBoardCommandRepository extends JpaRepository<AcquiredBoard, Long> {


}
