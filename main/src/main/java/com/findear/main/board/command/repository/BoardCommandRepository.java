package com.findear.main.board.command.repository;

import com.findear.main.board.common.domain.Board;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardCommandRepository extends JpaRepository<Board, Long> {
}
