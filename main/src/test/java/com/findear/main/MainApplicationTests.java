package com.findear.main;

import com.findear.main.support.IntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * 전체 컨텍스트가 뜨는지 확인한다. MySQL·Redis는 Testcontainers, 스키마는 Flyway(테스트 전용)로 만들고
 * ddl-auto: validate가 엔티티와 스키마가 맞는지 검증한다. Docker가 있어야 한다.
 */
@IntegrationTest
class MainApplicationTests {

	@Test
	void contextLoads() {
	}

}
