package com.findear.batch.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 통합 테스트용 MySQL·Elasticsearch 컨테이너. 이미지와 주요 설정은 compose.yml과 같다.
 * 컨테이너는 컨텍스트마다 하나씩 만들고 컨텍스트가 닫힐 때 함께 멈춘다 (같은 설정의 테스트 클래스는 컨텍스트 캐시로 공유).
 * @ServiceConnection이 datasource·elasticsearch 접속 정보를 자동으로 연결한다. 비정상 종료 때는 Testcontainers(Ryuk)가 정리한다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>("mysql:8.4.11")
                .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci", "--default-time-zone=+09:00");
    }

    // Docker Hub 공식 이미지(elasticsearch:8.19.22)를 Testcontainers의 ES 컨테이너로 쓴다. compose.yml과 같이 보안 off, 힙 512m
    @Bean
    @ServiceConnection
    ElasticsearchContainer elasticsearchContainer() {
        return new ElasticsearchContainer(DockerImageName.parse("elasticsearch:8.19.22")
                .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch"))
                .withEnv("xpack.security.enabled", "false")
                .withEnv("cluster.routing.allocation.disk.threshold_enabled", "false")
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");
    }
}
