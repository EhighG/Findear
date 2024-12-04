package com.findear.batch.repository;

import com.findear.batch.domain.FindearMatching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FindearMatchingRepository extends JpaRepository<FindearMatching, Long> {
    Page<FindearMatching> findAllByLostBoardIdOrderBySimilarityRateDesc(Pageable pageable, Long lostBoardId);
    List<FindearMatching> findAllByLostMemberId(Long lostMemberId);
}
