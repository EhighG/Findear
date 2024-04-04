package com.findear.batch.police.repository;

import com.findear.batch.police.domain.PoliceAcquiredData;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PoliceAcquiredDataRepository extends ElasticsearchRepository<PoliceAcquiredData, Long> {
}
