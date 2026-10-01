package com.findear.batch.support;

import com.findear.batch.ours.repository.FindearMatchingLogRepository;
import com.findear.batch.ours.repository.PoliceMatchingLogRepository;
import com.findear.batch.police.repository.PoliceAcquiredDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 전체 컨텍스트 + Testcontainers(MySQL·Elasticsearch) 통합 테스트의 공통 부모.
 * 스키마는 이 테스트에서만 Flyway로 infra/db/migration을 적용한다 (앱 본 설정은 flyway 미사용, 운영은 compose의 flyway 컨테이너, D-20).
 * 테스트 실행 디렉터리는 batch/ 이므로 마이그레이션 경로는 ../infra/db/migration 이다. 시드(infra/db/seed)는 적용하지 않는다.
 * match 서버는 mock(MatchMock)이고, 스케줄은 끈다. 모든 통합 테스트가 같은 컨텍스트(와 컨테이너)를 공유하며 테스트마다 데이터를 비운다.
 */
@SpringBootTest(properties = {
        "spring.datasource.password=test-only",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=filesystem:../infra/db/migration",
        "batch.scheduling.enabled=false"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
public abstract class IntegrationTestBase {

    protected static final MatchMock MATCH = new MatchMock();

    @DynamicPropertySource
    static void matchServer(DynamicPropertyRegistry registry) {
        registry.add("servers.match-server.url", MATCH::url);
    }

    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected FindearMatchingLogRepository findearMatchingLogRepository;
    @Autowired
    protected PoliceMatchingLogRepository policeMatchingLogRepository;
    @Autowired
    protected PoliceAcquiredDataRepository policeAcquiredDataRepository;

    @BeforeEach
    void cleanState() {
        MATCH.reset();
        jdbc.update("delete from tbl_img_file");
        jdbc.update("delete from tbl_lost_board");
        jdbc.update("delete from tbl_acquired_board");
        jdbc.update("delete from tbl_board");
        jdbc.update("delete from tbl_member");
        findearMatchingLogRepository.deleteAll();
        policeMatchingLogRepository.deleteAll();
        policeAcquiredDataRepository.deleteAll();
    }

    protected void insertMember(long id) {
        jdbc.update("insert into tbl_member (member_id, naver_uid, role, phone_number, withdrawal_yn) values (?, ?, 'NORMAL', ?, 0)",
                id, "test-uid-" + id, "010-0000-" + String.format("%04d", id));
    }

    protected void insertBoard(long boardId, long memberId, boolean lost, String category, String color,
                               String productName, String description, LocalDateTime registeredAt) {
        jdbc.update("insert into tbl_board (board_id, is_lost, ai_description, member_id, color, product_name, status, delete_yn, registered_at, category_name) "
                        + "values (?, ?, ?, ?, ?, ?, 'ONGOING', 0, ?, ?)",
                boardId, lost, description, memberId, color, productName, registeredAt, category);
    }

    protected void insertLostBoard(long lostBoardId, long boardId, LocalDate lostAt, float x, float y) {
        jdbc.update("insert into tbl_lost_board (lost_board_id, board_id, lost_at, suspicious_place, x_pos, y_pos) values (?, ?, ?, '테스트 장소', ?, ?)",
                lostBoardId, boardId, lostAt, x, y);
    }

    protected void insertAcquiredBoard(long acquiredBoardId, long boardId, float x, float y) {
        jdbc.update("insert into tbl_acquired_board (acquired_board_id, board_id, acquired_at, address, name, x_pos, y_pos) values (?, ?, CURRENT_DATE, '테스트 주소', '테스트 기관', ?, ?)",
                acquiredBoardId, boardId, x, y);
    }
}
