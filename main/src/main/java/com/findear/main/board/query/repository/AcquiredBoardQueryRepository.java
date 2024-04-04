package com.findear.main.board.query.repository;

import com.findear.main.board.common.domain.AcquiredBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcquiredBoardQueryRepository extends JpaRepository<AcquiredBoard, Long> {
    // 파이썬 서버 연동 후, 컬럼 채워지지 않은 데이터는 제거 조건 추가
    @Query("select ab from AcquiredBoard ab join fetch ab.board left join fetch ab.board.imgFileList where ab.board.id = :boardId and ab.board.deleteYn = false")
    Optional<AcquiredBoard> findByBoardId(Long boardId);

    @Query("select ab from AcquiredBoard ab join fetch ab.board left join fetch ab.board.imgFileList where ab.id = :acquiredBoardId and ab.board.deleteYn = false")
    Optional<AcquiredBoard> findById(Long acquiredBoardId);

    @Query("select ab from AcquiredBoard ab join fetch ab.board left join fetch ab.board.imgFileList where ab.board.deleteYn = false")
    List<AcquiredBoard> findAll();

    @Query("select ab from AcquiredBoard ab join fetch ab.board left join fetch ab.board.imgFileList where ab.board.deleteYn = false order by ab.id desc")
    List<AcquiredBoard> findAllDesc();

    @Query("select ab from AcquiredBoard ab join fetch ab.board left join fetch ab.board.imgFileList where ab.board.deleteYn = false order by ab.acquiredAt")
    List<AcquiredBoard> findAllOrderByAcquiredAt();

    @Query("select ab from AcquiredBoard ab join fetch ab.board left join fetch ab.board.imgFileList where ab.board.deleteYn = false order by ab.acquiredAt desc")
    List<AcquiredBoard> findAllOrderByAcquiredAtDesc();

}
