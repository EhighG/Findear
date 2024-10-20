package com.findear.stubbatchserver.repository;

import com.findear.stubbatchserver.domain.Lost112Matching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Lost112MatchingRepository extends JpaRepository<Lost112Matching, Long> {
}
