-- main 엔티티 13개 테이블 (main이 소유, batch는 일부 읽기: D-20, D-40)
-- 생성 도구: Hibernate ORM 6.6.53.Final (Spring Boot 3.5.16 관리 버전), dialect org.hibernate.dialect.MySQLDialect (MySQL 8.4)
--   naming strategy는 Spring Boot 기본값(CamelCaseToUnderscoresNamingStrategy + SpringImplicitNamingStrategy)
--   jakarta.persistence.schema-generation.scripts.action=create 로 뽑은 DDL (06-db-and-config.md §2 "V2 생성 방법")
-- 기준: master 브랜치 엔티티 (R-11b). 팀 DDL(Dump20240403.sql)은 리팩토링 이전이라 다르다
-- 컬럼 타입·NULL 여부·길이·기본값은 생성 결과 그대로다 (ddl-auto: validate 통과용, 바꾸지 않는다)
-- 생성 결과에서 정리한 것:
--   1) 테이블을 FK 의존 순서로 정렬, 컬럼은 PK → 엔티티 필드 순서로 정렬 (Hibernate는 타입별로 섞어서 출력)
--   2) 파일 안 순서를 CREATE TABLE → 인덱스·UNIQUE → FOREIGN KEY 로 나눔
--   3) Hibernate가 해시로 만든 FK·UK 이름을 fk_{테이블}_{컬럼}, uk_{테이블}_{컬럼} 으로 바꿈
--      (인덱스 ix_is_lost_delete_yn, ix_lost_at_board_id 는 엔티티에 지정된 이름 그대로)
-- 이미지 컬럼(tbl_img_file.img_url)은 현재 엔티티 그대로다. object key 저장으로 바꾸면서 컬럼명을 정리하는 것은
-- R-24가 V3 + 엔티티 변경으로 한다 (D-47)

-- ---- 테이블 ----

CREATE TABLE tbl_agency (
    agency_id bigint NOT NULL AUTO_INCREMENT,
    name varchar(255),
    x_pos float(23),
    y_pos float(23),
    address varchar(255),
    PRIMARY KEY (agency_id)
) ENGINE=InnoDB;

CREATE TABLE tbl_member (
    member_id bigint NOT NULL AUTO_INCREMENT,
    naver_uid varchar(255) NOT NULL,
    agency_id bigint,
    role enum ('MANAGER','NORMAL') NOT NULL,
    phone_number varchar(255) NOT NULL,
    joined_at datetime(6),
    withdrawal_at datetime(6),
    withdrawal_yn bit DEFAULT 0 NOT NULL,
    naver_refresh_token varchar(255),
    PRIMARY KEY (member_id)
) ENGINE=InnoDB;

CREATE TABLE tbl_board (
    board_id bigint NOT NULL AUTO_INCREMENT,
    is_lost bit NOT NULL,
    ai_description TEXT,
    member_id bigint,
    color varchar(255),
    product_name varchar(255),
    status enum ('DONE','ONGOING'),
    delete_yn bit DEFAULT 0 NOT NULL,
    registered_at datetime(6),
    thumbnail_url varchar(255),
    category_name varchar(255),
    PRIMARY KEY (board_id)
) ENGINE=InnoDB;

CREATE TABLE tbl_lost_board (
    lost_board_id bigint NOT NULL AUTO_INCREMENT,
    board_id bigint,
    lost_at date,
    suspicious_place varchar(255),
    x_pos float(23),
    y_pos float(23),
    PRIMARY KEY (lost_board_id)
) ENGINE=InnoDB;

CREATE TABLE tbl_acquired_board (
    acquired_board_id bigint NOT NULL AUTO_INCREMENT,
    board_id bigint,
    acquired_at date,
    address varchar(255),
    name varchar(255),
    x_pos float(23),
    y_pos float(23),
    PRIMARY KEY (acquired_board_id)
) ENGINE=InnoDB;

CREATE TABLE tbl_img_file (
    img_file_id bigint NOT NULL AUTO_INCREMENT,
    board_id bigint,
    img_url varchar(255),
    PRIMARY KEY (img_file_id)
) ENGINE=InnoDB;

CREATE TABLE tbl_scrap (
    scrap_id bigint NOT NULL AUTO_INCREMENT,
    board_id bigint,
    member_id bigint,
    PRIMARY KEY (scrap_id)
) ENGINE=InnoDB;

CREATE TABLE tbl_lost112_scrap (
    lost112_scrap_id bigint NOT NULL AUTO_INCREMENT,
    lost112_atc_id varchar(255),
    member_id bigint,
    PRIMARY KEY (lost112_scrap_id)
) ENGINE=InnoDB;

CREATE TABLE tbl_return_log (
    return_log_id bigint NOT NULL AUTO_INCREMENT,
    acquired_board_id bigint,
    phone_number varchar(255),
    returned_at datetime(6),
    cancel_at datetime(6),
    PRIMARY KEY (return_log_id)
) ENGINE=InnoDB;

CREATE TABLE tbl_message_room (
    message_room_id bigint NOT NULL AUTO_INCREMENT,
    member_id bigint,
    board_id bigint,
    PRIMARY KEY (message_room_id)
) ENGINE=InnoDB;

CREATE TABLE tbl_message (
    message_id bigint NOT NULL AUTO_INCREMENT,
    message_room_id bigint,
    title varchar(255),
    sender_id bigint,
    content TEXT,
    send_at datetime(6),
    PRIMARY KEY (message_id)
) ENGINE=InnoDB;

CREATE TABLE tbl_alarm (
    alarm_id bigint NOT NULL AUTO_INCREMENT,
    member_id bigint,
    author varchar(255),
    content varchar(255),
    generated_at varchar(255),
    read_yn bit,
    PRIMARY KEY (alarm_id)
) ENGINE=InnoDB;

CREATE TABLE tbl_notification (
    notification_id bigint NOT NULL AUTO_INCREMENT,
    member_id bigint,
    token varchar(255),
    PRIMARY KEY (notification_id)
) ENGINE=InnoDB;

-- ---- 인덱스·UNIQUE ----

CREATE INDEX ix_is_lost_delete_yn ON tbl_board (is_lost, delete_yn);
CREATE INDEX ix_lost_at_board_id ON tbl_lost_board (lost_at, board_id);

-- @OneToOne 관계는 Hibernate가 FK 컬럼에 UNIQUE를 건다
ALTER TABLE tbl_lost_board ADD CONSTRAINT uk_tbl_lost_board_board_id UNIQUE (board_id);
ALTER TABLE tbl_acquired_board ADD CONSTRAINT uk_tbl_acquired_board_board_id UNIQUE (board_id);
ALTER TABLE tbl_notification ADD CONSTRAINT uk_tbl_notification_member_id UNIQUE (member_id);

-- ---- 외래키 ----

ALTER TABLE tbl_member ADD CONSTRAINT fk_tbl_member_agency_id FOREIGN KEY (agency_id) REFERENCES tbl_agency (agency_id);
ALTER TABLE tbl_board ADD CONSTRAINT fk_tbl_board_member_id FOREIGN KEY (member_id) REFERENCES tbl_member (member_id);
ALTER TABLE tbl_lost_board ADD CONSTRAINT fk_tbl_lost_board_board_id FOREIGN KEY (board_id) REFERENCES tbl_board (board_id);
ALTER TABLE tbl_acquired_board ADD CONSTRAINT fk_tbl_acquired_board_board_id FOREIGN KEY (board_id) REFERENCES tbl_board (board_id);
ALTER TABLE tbl_img_file ADD CONSTRAINT fk_tbl_img_file_board_id FOREIGN KEY (board_id) REFERENCES tbl_board (board_id);
ALTER TABLE tbl_scrap ADD CONSTRAINT fk_tbl_scrap_board_id FOREIGN KEY (board_id) REFERENCES tbl_board (board_id);
ALTER TABLE tbl_scrap ADD CONSTRAINT fk_tbl_scrap_member_id FOREIGN KEY (member_id) REFERENCES tbl_member (member_id);
ALTER TABLE tbl_lost112_scrap ADD CONSTRAINT fk_tbl_lost112_scrap_member_id FOREIGN KEY (member_id) REFERENCES tbl_member (member_id);
ALTER TABLE tbl_return_log ADD CONSTRAINT fk_tbl_return_log_acquired_board_id FOREIGN KEY (acquired_board_id) REFERENCES tbl_acquired_board (acquired_board_id);
ALTER TABLE tbl_message_room ADD CONSTRAINT fk_tbl_message_room_member_id FOREIGN KEY (member_id) REFERENCES tbl_member (member_id);
ALTER TABLE tbl_message_room ADD CONSTRAINT fk_tbl_message_room_board_id FOREIGN KEY (board_id) REFERENCES tbl_board (board_id);
ALTER TABLE tbl_message ADD CONSTRAINT fk_tbl_message_message_room_id FOREIGN KEY (message_room_id) REFERENCES tbl_message_room (message_room_id);
ALTER TABLE tbl_alarm ADD CONSTRAINT fk_tbl_alarm_member_id FOREIGN KEY (member_id) REFERENCES tbl_member (member_id);
ALTER TABLE tbl_notification ADD CONSTRAINT fk_tbl_notification_member_id FOREIGN KEY (member_id) REFERENCES tbl_member (member_id);
