package com.findear.main.board.query.repository;

import com.findear.main.board.common.domain.LostBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LostBoardQueryRepository extends JpaRepository<LostBoard, Long>, LostBoardQueryCustomRepository {

    @Query("select lb from LostBoard lb join fetch lb.board left join fetch lb.board.imgFileList where lb.board.id = :boardId and lb.board.deleteYn = false")
    Optional<LostBoard> findByBoardId(Long boardId);

    @Query("select lb from LostBoard lb join fetch lb.board left join fetch lb.board.imgFileList where lb.id = :lostBoardId and lb.board.deleteYn = false")
    Optional<LostBoard> findById(Long lostBoardId);

}
