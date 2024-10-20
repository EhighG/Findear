package com.findear.stubbatchserver.repository;

import com.findear.stubbatchserver.domain.FindearMatching;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FindearMatchingRepository extends JpaRepository<FindearMatching, Long> {

}
