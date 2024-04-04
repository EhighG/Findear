package com.findear.main.board.command.repository;

import com.findear.main.board.common.domain.Board;
import com.findear.main.board.common.domain.LostBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LostBoardCommandRepository extends JpaRepository<LostBoard, Long> {
}
