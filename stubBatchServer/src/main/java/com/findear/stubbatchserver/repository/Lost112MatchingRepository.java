package com.findear.stubbatchserver.repository;

import com.findear.stubbatchserver.domain.Lost112Matching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Lost112MatchingRepository extends JpaRepository<Lost112Matching, Long> {

    @Query("SELECT lm FROM Lost112Matching lm JOIN FETCH lm.lost112Acquired WHERE lm.lostBoardId = :lostBoardId ORDER BY lm.similarityRate DESC")
    Page<Lost112Matching> findAllByLostBoardIdOrderBySimilarityRateDesc(Pageable pageable, Long lostBoardId);
    @Query("SELECT lm FROM Lost112Matching lm JOIN FETCH lm.lost112Acquired WHERE lm.lostMemberId = :lostMemberId")
    List<Lost112Matching> findAllByLostMemberId(Long lostMemberId);
}
