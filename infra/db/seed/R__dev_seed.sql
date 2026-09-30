-- 개발용 소량 시드 (로컬 전용, D-48)
-- Flyway 반복 마이그레이션(R__)이다. compose.override.yml이 flyway의 locations에 이 폴더를 추가할 때만 적용된다.
-- 배포(compose.yml + compose.prod.yml)에서는 이 폴더를 마운트하지 않으므로 적용되지 않는다.
-- 멱등: 고정 PK + INSERT ... ON DUPLICATE KEY UPDATE 라서 몇 번을 다시 실행해도 행 수와 결과가 같다.
--   (파일 내용이 바뀌면 Flyway가 다시 적용한다. 고정 PK 1~4번을 이미 앱이 쓰고 있으면 그 행을 덮어쓰므로 빈 DB에서 쓰는 것을 전제로 한다.)
-- 내용: 기관 1, 회원 2(NORMAL·MANAGER), 분실물 2, 습득물 2. 이미지·쪽지·알림·스크랩은 넣지 않는다.
-- 날짜는 실행 시점 기준 상대값이다. UPSERT는 MySQL 8.0.19+의 행 별칭(AS new_row) 문법이다 (VALUES(col)은 deprecated).
--
-- 테스트 로그인 (local 프로필, R-27 이후 local 한정):
--   POST /members/login  {"phoneNumber": "010-0000-0001"}   -- NORMAL 회원
--   POST /members/login  {"phoneNumber": "010-0000-0002"}   -- MANAGER 회원 (서울역 유실물센터 소속)

-- mysql 클라이언트로 직접 실행할 때도 한글이 깨지지 않게 (컨테이너 클라이언트 기본값은 latin1)
SET NAMES utf8mb4;

-- 기관
INSERT INTO tbl_agency (agency_id, name, x_pos, y_pos, address)
VALUES (1, '서울역 유실물센터', 126.9707, 37.5547, '서울특별시 용산구 한강대로 405 서울역')
AS new_row
ON DUPLICATE KEY UPDATE
    name = new_row.name, x_pos = new_row.x_pos, y_pos = new_row.y_pos, address = new_row.address;

-- 회원
INSERT INTO tbl_member (member_id, naver_uid, agency_id, role, phone_number, joined_at, withdrawal_at, withdrawal_yn, naver_refresh_token)
VALUES
    (1, 'local-normal-1',  NULL, 'NORMAL',  '010-0000-0001', NOW(6) - INTERVAL 30 DAY, NULL, 0, NULL),
    (2, 'local-manager-1', 1,    'MANAGER', '010-0000-0002', NOW(6) - INTERVAL 30 DAY, NULL, 0, NULL)
AS new_row
ON DUPLICATE KEY UPDATE
    naver_uid = new_row.naver_uid, agency_id = new_row.agency_id, role = new_row.role,
    phone_number = new_row.phone_number, joined_at = new_row.joined_at, withdrawal_at = new_row.withdrawal_at,
    withdrawal_yn = new_row.withdrawal_yn, naver_refresh_token = new_row.naver_refresh_token;

-- 게시글 공통 (1, 2 = 분실물 / NORMAL 회원, 3, 4 = 습득물 / MANAGER 회원)
INSERT INTO tbl_board (board_id, is_lost, ai_description, member_id, color, product_name, status, delete_yn, registered_at, thumbnail_url, category_name)
VALUES
    (1, 1, '검정 가죽 지갑 카드 수납',     1, '검정', '검은색 가죽 지갑',     'ONGOING', 0, NOW(6) - INTERVAL 3 DAY, NULL, '지갑'),
    (2, 1, '하얀색 이어폰 충전 케이스',    1, '흰색', '무선 이어폰 케이스',   'ONGOING', 0, NOW(6) - INTERVAL 2 DAY, NULL, '전자기기'),
    (3, 0, '검정 가죽 지갑 반지갑',        2, '검정', '검은색 반지갑',        'ONGOING', 0, NOW(6) - INTERVAL 2 DAY, NULL, '지갑'),
    (4, 0, '하얀색 무선 이어폰 케이스',    2, '흰색', '무선 이어폰 케이스',   'ONGOING', 0, NOW(6) - INTERVAL 1 DAY, NULL, '전자기기')
AS new_row
ON DUPLICATE KEY UPDATE
    is_lost = new_row.is_lost, ai_description = new_row.ai_description, member_id = new_row.member_id,
    color = new_row.color, product_name = new_row.product_name, status = new_row.status,
    delete_yn = new_row.delete_yn, registered_at = new_row.registered_at,
    thumbnail_url = new_row.thumbnail_url, category_name = new_row.category_name;

-- 분실물
INSERT INTO tbl_lost_board (lost_board_id, board_id, lost_at, suspicious_place, x_pos, y_pos)
VALUES
    (1, 1, CURRENT_DATE - INTERVAL 3 DAY, '서울역 1번 출구 앞', 126.9707, 37.5547),
    (2, 2, CURRENT_DATE - INTERVAL 2 DAY, '강남역 지하상가',    127.0276, 37.4979)
AS new_row
ON DUPLICATE KEY UPDATE
    board_id = new_row.board_id, lost_at = new_row.lost_at, suspicious_place = new_row.suspicious_place,
    x_pos = new_row.x_pos, y_pos = new_row.y_pos;

-- 습득물
INSERT INTO tbl_acquired_board (acquired_board_id, board_id, acquired_at, address, name, x_pos, y_pos)
VALUES
    (1, 3, CURRENT_DATE - INTERVAL 2 DAY, '서울특별시 용산구 한강대로 405 서울역 대합실', '서울역 유실물센터', 126.9707, 37.5547),
    (2, 4, CURRENT_DATE - INTERVAL 1 DAY, '서울특별시 용산구 한강대로 405 서울역 대합실', '서울역 유실물센터', 126.9707, 37.5547)
AS new_row
ON DUPLICATE KEY UPDATE
    board_id = new_row.board_id, acquired_at = new_row.acquired_at, address = new_row.address,
    name = new_row.name, x_pos = new_row.x_pos, y_pos = new_row.y_pos;
