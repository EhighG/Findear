package com.findear.stubbatchserver.repository;

import com.findear.stubbatchserver.domain.FindearMatching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FindearMatchingRepository extends JpaRepository<FindearMatching, Long> {
    Page<FindearMatching> findAllByLostBoardIdOrderBySimilarityRateDesc(Pageable pageable, Long lostBoardId);
}
