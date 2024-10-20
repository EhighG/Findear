package com.findear.stubbatchserver.repository;

import com.findear.stubbatchserver.domain.Lost112Acquired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Lost112AcquiredRepository extends JpaRepository<Lost112Acquired, Long> {
}
