package com.findear.batch.job;

import com.findear.batch.support.IntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Isolation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 잡 생성 격리 수준 설정 확인 (R-34): MySQL에서 기본값(SERIALIZABLE)은 두 잡이 같은 초에 시작하면 BATCH_JOB_INSTANCE 교착을 일으킨다.
 * spring.batch.jdbc.isolation-level-for-create=read_committed가 설정 속성과 실제 JobRepository 구성(Boot의 배치 구성) 모두에 적용됐는지 본다.
 */
class BatchIsolationConfigTest extends IntegrationTestBase {

    @Autowired
    BatchProperties batchProperties;
    @Autowired
    DefaultBatchConfiguration batchConfiguration;

    @DisplayName("잡 생성 격리 수준은 READ_COMMITTED (설정 속성과 JobRepository 구성 모두)")
    @Test
    void isolationLevelForCreateIsReadCommitted() {
        assertThat(batchProperties.getJdbc().getIsolationLevelForCreate()).isEqualTo(Isolation.READ_COMMITTED);
        assertThat((Object) ReflectionTestUtils.invokeMethod(batchConfiguration, "getIsolationLevelForCreate")).isEqualTo(Isolation.READ_COMMITTED);
    }
}
