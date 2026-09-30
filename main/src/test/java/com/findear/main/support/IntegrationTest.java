package com.findear.main.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 전체 컨텍스트 + Testcontainers(MySQL·Redis) + MockMvc(Security 필터 포함) 통합 테스트.
 * 스키마는 이 테스트에서만 Flyway로 infra/db/migration을 적용한다 (앱 본 설정은 flyway 미사용, 운영은 compose의 flyway 컨테이너, D-20).
 * 테스트 실행 디렉터리는 main/ 이므로 마이그레이션 경로는 ../infra/db/migration 이다. 시드(infra/db/seed)는 적용하지 않는다.
 * 필수 속성(JWT_SECRET 등)은 여기서 테스트 값으로 준다 (실제 비밀값 아님). 프로필은 각 테스트가 @ActiveProfiles로 고른다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest(properties = {
        "jwt-secret=dGVzdC1qd3Qtc2VjcmV0LXRlc3Qtand0LXNlY3JldC0xMjM0NTY=",
        "spring.datasource.password=test-only",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=filesystem:../infra/db/migration"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
public @interface IntegrationTest {
}
