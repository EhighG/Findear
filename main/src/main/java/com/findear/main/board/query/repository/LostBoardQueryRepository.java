package com.findear.main.board.query.repository;

import com.findear.main.board.common.domain.LostBoard;
import com.findear.main.board.common.domain.Board;
import com.findear.main.board.common.domain.LostBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LostBoardQueryRepository extends JpaRepository<LostBoard, Long> {

    @Query("select lb from LostBoard lb join fetch lb.board left join fetch lb.board.imgFileList where lb.board.id = :boardId and lb.board.deleteYn = false")
    Optional<LostBoard> findByBoardId(Long boardId);

    @Query("select lb from LostBoard lb join fetch lb.board left join fetch lb.board.imgFileList where lb.id = :lostBoardId and lb.board.deleteYn = false")
    Optional<LostBoard> findById(Long lostBoardId);

    // delete_yn 기본값 적용 시 where 조건 바꾸기
    @Query("select lb from LostBoard lb join fetch lb.board left join fetch lb.board.imgFileList where lb.board.deleteYn = false")
    List<LostBoard> findAll();
    @Query("select lb from LostBoard lb join fetch lb.board left join fetch lb.board.imgFileList where lb.board.deleteYn = false order by lb.lostAt")
    List<LostBoard> findAllOrderByLostAt();

    @Query("select lb from LostBoard lb join fetch lb.board left join fetch lb.board.imgFileList where lb.board.deleteYn = false order by lb.lostAt desc")
    List<LostBoard> findAllOrderByLostAtDesc();

}
