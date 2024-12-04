package com.findear.batch.ours.repository;

import com.findear.batch.ours.domain.PoliceMatchingLog;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PoliceMatchingLogRepository extends ElasticsearchRepository<PoliceMatchingLog, Long> {

}
