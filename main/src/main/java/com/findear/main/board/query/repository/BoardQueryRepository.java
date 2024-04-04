package com.findear.main.board.query.repository;

import com.findear.main.board.common.domain.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoardQueryRepository extends JpaRepository<Board, Long> {

    Optional<Board> findByIdAndDeleteYnFalse(Long boardId);
}
