package com.findear.main.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

/**
 * 통합 테스트용 MySQL·Redis 컨테이너. 이미지는 compose.yml과 같은 버전이다.
 * 컨테이너는 컨텍스트마다 하나씩 만들고 컨텍스트가 닫힐 때 함께 멈춘다 (프로필별 컨텍스트가 각자 자기 컨테이너를 쓰므로
 * 먼저 닫힌 컨텍스트가 다른 컨텍스트의 접속을 끊지 않는다). 같은 설정의 테스트 클래스는 컨텍스트 캐시로 컨테이너를 공유한다.
 * @ServiceConnection이 datasource·redis 접속 정보를 자동으로 연결한다. 비정상 종료 때는 Testcontainers(Ryuk)가 정리한다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    MySQLContainer<?> mysqlContainer() {
        return new MySQLContainer<>("mysql:8.4.11")
                .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_0900_ai_ci", "--default-time-zone=+09:00");
    }

    // compose.yml과 같이 영속화를 끈다 (D-25)
    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>("redis:8.8.3")
                .withExposedPorts(6379)
                .withCommand("redis-server", "--save", "", "--appendonly", "no");
    }
}
