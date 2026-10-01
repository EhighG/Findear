package com.findear.batch;

import com.findear.batch.ours.job.scheduler.FindearJobScheduler;
import com.findear.batch.police.job.scheduler.PoliceJobScheduler;
import com.findear.batch.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 컨텍스트 기동 테스트: Flyway(V1~V3) 스키마에 대해 JPA ddl-auto: validate가 통과하고 ES 리포지토리(인덱스)가 초기화된다.
 * 엔티티 매핑이 스키마와 다르면 이 테스트가 컨텍스트 기동 단계에서 실패한다.
 */
class BatchApplicationTests extends IntegrationTestBase {

    @Autowired
    ApplicationContext context;
    @Autowired
    ElasticsearchOperations elasticsearchOperations;

    @DisplayName("Flyway 스키마에 validate가 통과하고 컨텍스트가 뜬다")
    @Test
    void contextLoads() {
        assertThat(context.getBean("findearJob")).isNotNull();
        assertThat(context.getBean("policeJob")).isNotNull();
    }

    @DisplayName("ES 인덱스 3개가 리포지토리 초기화 때 만들어진다")
    @Test
    void indicesCreated() {
        for (String index : new String[]{"police_acquired_data", "findear_matching_log", "police_matching_log"}) {
            assertThat(elasticsearchOperations.indexOps(IndexCoordinates.of(index)).exists()).as(index).isTrue();
        }
    }

    @DisplayName("통합 테스트 컨텍스트는 batch.scheduling.enabled=false라 스케줄러 빈이 없다")
    @Test
    void schedulersAreOffInTests() {
        assertThat(context.getBeansOfType(FindearJobScheduler.class)).isEmpty();
        assertThat(context.getBeansOfType(PoliceJobScheduler.class)).isEmpty();
    }
}
