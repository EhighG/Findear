package com.findear.batch.ours.repository;

import com.findear.batch.ours.domain.FindearMatchingLog;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FindearMatchingLogRepository extends ElasticsearchRepository<FindearMatchingLog, Long> {
}
