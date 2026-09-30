# 06. DB·데이터 저장소·설정

## 1. MySQL

| 항목 | 값 |
|---|---|
| 이미지 | `mysql:8.4.11` (D-03) |
| DB | `findear` — main·batch 공유 (D-04) |
| 계정 | 앱 `findear`(`MYSQL_USER`), root(`MYSQL_ROOT_PASSWORD`), 모니터링 `exporter`(`MYSQL_EXPORTER_PASSWORD`, PROCESS·REPLICATION CLIENT·SELECT) |
| 문자셋·시간대 | `utf8mb4` / `utf8mb4_0900_ai_ci`, `--default-time-zone=+09:00`, `TZ=Asia/Seoul` |
| JDBC URL | `jdbc:mysql://${DB_HOST}:${DB_PORT}/${MYSQL_DATABASE}?serverTimezone=Asia/Seoul&characterEncoding=UTF-8` |
| 인증 방식 | MySQL 8.4 기본값 `caching_sha2_password`. MySQL Connector/J(main·batch)는 기본 SSL(`sslMode=PREFERRED`)로 통과하지만 SSL을 끄면 `allowPublicKeyRetrieval=true`가 필요하다. Flyway 이미지는 MariaDB Connector/J 2.7만 들어 있어서 `flyway`의 URL에 `allowPublicKeyRetrieval=true`를 붙였다 (R-11a, compose 내부망에서만 사용) |
| 초기화 스크립트 | `infra/mysql/initdb/` — exporter 계정 생성 (볼륨이 비어 있을 때 최초 1회만 실행) |

## 2. 스키마 관리 (Flyway, D-20)

- 위치: `infra/db/migration/`, 파일명 `V{번호}__{설명}.sql`
- 실행: compose의 `flyway` one-shot 서비스 → main·batch는 `depends_on: flyway (service_completed_successfully)`
- 앱 설정: main·batch 모두 `spring.jpa.hibernate.ddl-auto: validate`, `spring.flyway.enabled: false`(앱 내부 Flyway 미사용)

### 마이그레이션 계획
| 버전 | 내용 | 작성 방법 | 작업 |
|---|---|---|---|
| V1__spring_batch_schema.sql | Spring Batch 5.2 메타 테이블 | Boot 3.5.x가 관리하는 `spring-batch-core` jar 안의 `org/springframework/batch/core/schema-mysql.sql` 복사 | R-11a (Phase 1) |
| V2__init_schema.sql | main 엔티티 13개 테이블 + 인덱스 + 기본값 | 아래 "V2(main 스키마) 생성 방법" | R-11b (Phase 2, R-20 다음) |
| V3 이후 | 이후 변경 (예: 이미지 key 컬럼명 정리, 인덱스 추가) | 수동 작성 | – |

- 번호 순서가 원래 계획(V1=main, V2=batch 메타)과 반대인 이유 (D-40): main 스키마는 R-20(Boot 3.5) 이후에만 만들 수 있는데, Flyway는 이미 적용된 버전보다 낮은 번호를 나중에 추가하면 검증에 실패한다(`outOfOrder` 미사용). 그래서 Phase 1에서 먼저 적용하는 batch 메타 스키마가 V1.

### V2(main 스키마) 생성 방법
1. **master 엔티티를 기준**으로 만듭니다 (팀 DDL은 리팩토링 이전이라 다름). Boot 3.5로 올린 뒤(R-20) 같은 Hibernate 버전(6.6.x)으로 생성해야 `validate` 타입 불일치가 없습니다 (예: Boolean → `bit(1)`, `@Enumerated(STRING)` → MySQL `enum(...)`).
2. 빈 DB에 대해 스키마 스크립트 출력:
   ```yaml
   spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action: create
   spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target: build/V2__init_schema.sql
   spring.jpa.properties.hibernate.hbm2ddl.delimiter: ";"
   ```
   - 실제로 한 방법 (R-11b): 앱을 띄우지 않고 임시 JUnit 테스트에서 `MetadataSources`에 엔티티 13개를 등록하고 위 `scripts.*` 설정으로 DDL을 뽑았다. DB 접속 없이 `hibernate.dialect=org.hibernate.dialect.MySQLDialect`, `hibernate.boot.allow_jdbc_metadata_access=false`, `jakarta.persistence.database-product-name=MySQL`(버전 8.4), naming strategy는 Boot 기본값(`CamelCaseToUnderscoresNamingStrategy`, `SpringImplicitNamingStrategy`). Hibernate 6.6에는 `SchemaExport`(`org.hibernate.tool.hbm2ddl`)가 없다. 임시 코드는 커밋하지 않았다
3. 결과를 검토하고 정리: 테이블·컬럼 순서, 인덱스 이름(`ix_is_lost_delete_yn`, `ix_lost_at_board_id`), 기본값(`delete_yn`, `withdrawal_yn` = 0), 외래키 이름. R-11b에서는 테이블을 FK 의존 순으로, 파일을 CREATE → 인덱스·UNIQUE → FK 순으로 나누고, Hibernate가 해시로 지은 FK 14개·UK 3개 이름을 `fk_{테이블}_{컬럼}`, `uk_{테이블}_{컬럼}`으로 바꿨다. **타입·NULL·길이·기본값은 생성값 그대로** 둔다 (바꾸면 `validate`에서 어긋남). 같은 DB에 대해 Hibernate `validate`가 통과하는 것을 임시 테스트로 확인함
4. 대조 자료: 팀 DDL `git show 2af1413:exec/Dump20240403.sql`, MariaDB용 DDL `git show 63ba032:exec/ddl_mariaDB_10.11.8.sql`.
5. 리팩토링으로 바뀐 점: `tbl_member.password` 삭제, `tbl_board`·`tbl_lost_board` 인덱스 추가, 기본값 추가.
6. batch도 같은 테이블을 매핑하는 엔티티를 가지고 있음(`ours/domain`, `alarm/domain`) → batch를 `validate`로 기동해 호환 확인 (R-30).
7. 엔티티를 바꾸면 V2를 고치지 않고 V3 이후를 수동으로 작성한다 (이미 적용된 DB의 체크섬 검증 때문). 작성한 뒤 위 방법으로 생성한 DDL과 비교하면 타입 불일치를 미리 잡을 수 있다.

### 테이블 목록
| 테이블 | 소유(쓰기) | batch 사용 |
|---|---|---|
| `tbl_member` | main | 읽기 (`Member`) |
| `tbl_agency` | main | – |
| `tbl_board` | main | 읽기 (`Board`) |
| `tbl_lost_board` | main | 읽기 (`LostBoard`) |
| `tbl_acquired_board` | main | 읽기 (`AcquiredBoard`) |
| `tbl_img_file` | main | 읽기 (`imgFile`) |
| `tbl_scrap`, `tbl_lost112_scrap`, `tbl_return_log` | main | – |
| `tbl_message`, `tbl_message_room` | main | – |
| `tbl_alarm` | main | 엔티티 매핑만 있음(`Member.alarmList`), 쓰기 없음 → batch 쪽 `alarm` 패키지는 주석 코드라 정리 대상 |
| `tbl_notification` | main | – |
| `BATCH_*` (Spring Batch 메타) | batch | 읽기·쓰기 |

## 3. Elasticsearch (D-06)

- 이미지 `elasticsearch:8.19.22`, single-node. 로컬은 `xpack.security.enabled=false`, 배포는 security on + `ELASTIC_PASSWORD`.
- 메모리 (D-31): 힙 512m 고정만 하고 기능은 기본값 유지.
- 한국어 형태소 분석(nori)은 플러그인이 필요해 기본 이미지로는 standard analyzer 사용. 검색 품질을 높이려면 nori 플러그인을 넣은 커스텀 이미지 검토(선택).
- 인덱스 매핑은 앱 쪽 Spring Data ES 어노테이션(`@Document`, `@Field`, `@Setting`)으로 명시합니다 (팀 시절은 자동 매핑이었음).

| 인덱스 | 문서 ID (변경) | 주요 필드와 매핑 |
|---|---|---|
| `police_acquired_data` | `atcId` (자연키, 중복 방지) | `atcId` keyword, `depPlace` text+keyword, `addr` text, `fdFilePathImg` keyword(index false), `fdPrdtNm` text+keyword, `fdSbjt` text, `clrNm` keyword, `fdYmd` **date(`yyyy-MM-dd`)**, `prdtClNm`·`mainPrdtClNm`·`subPrdtClNm` keyword, (신규 선택) `source` keyword(경찰/포털) |
| `findear_matching_log` | `{lostBoardId}-{acquiredBoardId}` | `lostBoardId`·`acquiredBoardId` long, `similarityRate` float, `matchingAt` date |
| `police_matching_log` | `{lostBoardId}-{atcId}` | `lostBoardId` long, `similarityRate` float, `matchingAt` date, 습득물 필드 사본(`atcId`, `depPlace`, `fdFilePathImg`, `fdPrdtNm`, `fdSbjt`, `clrNm`, `fdYmd`, `mainPrdtClNm`) |

- 팀 코드는 문서 ID를 `count()+1`로 부여해 동시 실행 시 충돌 → 위 결정적 ID로 변경 (재매칭 시 덮어쓰기 = upsert).
- 전체 재적재가 필요하면 "새 인덱스에 적재 → alias 교체" 방식 권장 (조회 중단 없음).

## 4. 스토리지 (S3 호환)

| 항목 | 로컬 (SeaweedFS) | 배포 (AWS S3) |
|---|---|---|
| 버킷 | `findear-images` (`STORAGE_BUCKET`) | 사용자가 정한 버킷 이름 |
| 서버용 엔드포인트 `STORAGE_ENDPOINT` | `http://seaweedfs:8333` (IDE 실행 시 `http://localhost:8333`) | 비움 (SDK 기본) |
| presigned URL용 엔드포인트 `STORAGE_PUBLIC_ENDPOINT` | `http://localhost:8333` | 비움 |
| 조회 URL prefix `STORAGE_PUBLIC_BASE_URL` | `http://localhost:8333/findear-images` | `https://{bucket}.s3.ap-northeast-2.amazonaws.com` (또는 CloudFront) |
| path-style `STORAGE_PATH_STYLE` | `true` | `false` |
| 자격증명 | `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` (SeaweedFS용 임의값) | **EC2 IAM Role** (키를 `.env`에 넣지 않음, SDK 기본 자격증명 체인) |

- 객체 키: `images/{yyyy}/{MM}/{uuid}.{ext}`. 공개 읽기는 `images/*`에만: 로컬·AWS 모두 같은 **버킷 정책**(`Principal: *`, `s3:GetObject`, `arn:aws:s3:::{bucket}/images/*`)으로 준다 (D-45, `infra/seaweedfs/storage-init.sh`). 나머지 경로·쓰기·목록 조회는 자격증명이 필요하다. AWS는 새 버킷에 Block Public Access가 기본으로 켜져 있어 정책을 넣기 전에 `put-public-access-block`으로 정책 기반 공개를 허용해야 한다 (AWS 전용 단계, R-64)
- DB(`tbl_img_file`)에는 **object key** 저장, API 응답에서 `STORAGE_PUBLIC_BASE_URL`과 합쳐 URL 반환 (D-13). V2(R-11b)의 `tbl_img_file`은 현재 엔티티 그대로 `img_url`이고, object key 저장으로 바꾸면서 컬럼명을 정리하는 것은 R-24가 V3 + 엔티티 변경으로 한다 (D-47).
- presigned PUT 만료 `STORAGE_PRESIGN_EXPIRE_SECONDS`(기본 600). Content-Type과 최대 크기(10MB, 기존 multipart 제한과 동일)를 서명에 포함.
- **구현 (R-24)**: `main/.../storage/`(`StorageProperties`, `StorageConfig`, `ImageStorageService`, `ImageUrls`). AWS SDK v2(BOM 2.55.8) — `S3Client`는 `STORAGE_ENDPOINT`(비면 AWS 기본), `S3Presigner`는 `STORAGE_PUBLIC_ENDPOINT`로 서명, `STORAGE_PATH_STYLE`, 자격증명은 SDK 기본 체인(배포는 EC2 IAM Role). 모드 B에서는 `.env`가 Spring 속성으로만 들어오므로 **local 프로필에서만** `AWS_ACCESS_KEY_ID`/`SECRET`을 `StaticCredentialsProvider`로 넘긴다. `S3Client`는 `requestChecksumCalculation(WHEN_REQUIRED)`(S3 호환 서버 대비). 컬럼은 V3에서 `tbl_img_file.img_key`, `tbl_board.thumbnail_key` (D-47)
- **AWS 전환 시 IAM 권한 (R-64)**: presigned PUT은 서버 자격증명으로 서명하므로 서버 Role에 `s3:PutObject`(`images/*`)가 있어야 브라우저 업로드가 된다. 등록 때 key 존재 확인(`HeadObject`)에는 `s3:GetObject`와 **`s3:ListBucket`**이 필요하다 — `ListBucket`이 없으면 AWS는 없는 객체를 404가 아니라 403으로 답하고, 지금 구현은 403을 저장소 오류로 처리한다.
- **남은 한계 (1차 이후)**: 수정·삭제로 떨어진 옛 객체와 presign만 받고 쓰지 않은 객체는 스토리지에 남는다(정리 작업 없음). presign 발급자와 등록자가 같은지는 확인하지 않는다(key는 UUID라 추측은 어려움).
- CORS: 브라우저 업로드를 위해 `PUT`, `GET`, `HEAD` 허용, origin은 `CORS_ALLOWED_ORIGINS`와 동일하게. SeaweedFS도 `put-bucket-cors`를 지원한다 (R-13: 허용 origin만 preflight 통과, 다른 origin은 403).

## 5. Redis

- 이미지 `redis:8.8.3`, 영속화 없음 (D-25). 배포는 `--requirepass ${REDIS_PASSWORD}`.
- 키: memberId → refresh token, TTL 1439분 (`security/RefreshTokenRepository`).
- 키 형식 (R-27): `StringRedisTemplate`으로 `refresh:{memberId}`, TTL 1439분 — redis-cli에서 `KEYS refresh:*`로 읽힌다 (이전에는 JDK 직렬화 키).

## 6. 환경변수 전체 목록

이 표가 최종 목록입니다. `.env.example`에는 각 R-xx가 쓰기 시작하는 변수를 그때 추가하고, R-90에서 이 표와 일치하는지 확인합니다 (D-43). "비밀"은 실제 값을 커밋하면 안 되는 항목입니다.

### 공통 / compose
| 변수 | 사용처 | `.env.example` 값 | 비밀 | 비고 |
|---|---|---|---|---|
| `COMPOSE_PROJECT_NAME` | compose | `findear` | | |
| `COMPOSE_PROFILES` | compose | `monitoring` | | 비우면 모니터링 제외 |
| `TZ` | 전체 | `Asia/Seoul` | | |
| `SPRING_PROFILES_ACTIVE` | main, batch, match | `local` | | 배포는 `prod` |
| `*_MEM_LIMIT` | compose | **최소값** ([04 §5](04-target-architecture.md#5-리소스-산정-메모리)의 "최소(기본값)" 열, D-31) | | `MAIN_MEM_LIMIT`(512m), `BATCH_MEM_LIMIT`(512m), `MATCH_MEM_LIMIT`(256m), `MYSQL_MEM_LIMIT`(512m), `REDIS_MEM_LIMIT`(64m), `ES_MEM_LIMIT`(1g), `SEAWEEDFS_MEM_LIMIT`(128m), `PROMETHEUS_MEM_LIMIT`(256m), `GRAFANA_MEM_LIMIT`(192m), `CADVISOR_MEM_LIMIT`(128m), `EXPORTER_MEM_LIMIT`(32m) |

### 로컬 호스트 포트 (`compose.override.yml`, D-44)
| 변수 | `.env.example` 값 | 비고 |
|---|---|---|
| `MYSQL_HOST_PORT` | `3306` | 이미 쓰는 포트면 변경 (개발 PC는 `3307`). 실행 모드 B에서는 앱의 `DB_PORT`도 같은 값 |
| `REDIS_HOST_PORT` | `6379` | 모드 B에서는 `REDIS_PORT`도 같은 값 |
| `ES_HOST_PORT` | `9200` | 모드 B에서는 `ELASTICSEARCH_URIS`의 포트도 같은 값 |
| `SEAWEEDFS_HOST_PORT` | `8333` | 바꾸면 `STORAGE_PUBLIC_ENDPOINT`·`STORAGE_PUBLIC_BASE_URL`의 포트도 같은 값 (presigned URL의 Host) |
| `PROMETHEUS_HOST_PORT` / `GRAFANA_HOST_PORT` | `9090` / `3000` | R-14 |
| `MAIN_HOST_PORT` | `8080` | R-21. 이미 쓰는 포트면 변경 (개발 PC는 `8090`). 배포(`compose.prod.yml`)에서는 `80` |
| `MATCH_HOST_PORT` | `8084` | R-40. 디버깅용 (main·batch는 compose 내부 주소 `http://match:8084`로 호출) |
| (앱) | – | batch는 Phase 3에서 같은 방식으로 추가 |

### DB / 캐시 / 검색
| 변수 | 사용처 | `.env.example` 값 | 비밀 | 비고 |
|---|---|---|---|---|
| `MYSQL_DATABASE` | mysql, flyway, main, batch | `findear` | | |
| `MYSQL_USER` / `MYSQL_PASSWORD` | 〃 | `findear` / (임의) | O | |
| `MYSQL_ROOT_PASSWORD` | mysql | (임의) | O | |
| `MYSQL_EXPORTER_PASSWORD` | mysql initdb, mysqld-exporter | (임의) | O | |
| `DB_HOST` / `DB_PORT` | main, batch | compose가 `mysql`/`3306` 주입 | | 앱 기본값 `localhost` |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | main, redis, redis-exporter | compose가 `redis` 주입 / `6379` / 빈 값 | O | 배포는 비밀번호 필수 |
| `ELASTICSEARCH_URIS` | batch, es-exporter | compose가 `http://elasticsearch:9200` 주입 | | 앱 기본값 `http://localhost:9200` |
| `ES_JAVA_OPTS` | elasticsearch | `-Xms512m -Xmx512m` | | 최소 사양 (D-31). `ES_MEM_LIMIT`을 올릴 때 같이 올림 (힙 ≤ 제한의 절반) |
| `ELASTIC_PASSWORD` | elasticsearch, batch, es-exporter | 빈 값 | O | 배포에서 security on |

### main
| 변수 | `.env.example` 값 | 비밀 | 비고 |
|---|---|---|---|
| `JWT_SECRET` | (임의, 256bit 이상 Base64) | O | |
| `JWT_ACCESS_TTL_MS` | `7200000` | | 2시간 |
| `NAVER_CLIENT_ID` / `NAVER_CLIENT_SECRET` | (발급) | O | U-06 |
| `NAVER_REDIRECT_URI` | `http://localhost:8080/members/login` | | |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:5500` | | 5500 = FCM 테스트 페이지 |
| `VWORLD_API_KEY` | (발급) | O | U-07 |
| `FCM_ENABLED` | `false` | | 키 준비 후 `true` |
| `FCM_CREDENTIALS_PATH` | `/run/secrets/firebase-adminsdk.json` | | 파일은 `./secrets/`에 두고 마운트 |
| `BATCH_SERVER_URL` | compose가 `http://batch:8082` 주입 | | 앱 기본값 `http://localhost:8082` |
| (외부 API 주소) | – | | VWorld·Naver 주소는 `.env`에 넣지 않고 설정 파일에 공식 주소를 기본값으로 둔다. 테스트에서만 mock 서버 주소로 교체 (D-38) |
| `MATCH_SERVER_URL` | compose가 `http://match:8084` 주입 | | 앱 기본값 `http://localhost:8084` (batch도 사용). 습득물 자동채움 응답 대기 상한은 설정 `servers.match-server.autofill-timeout`(30s, 환경변수 없음, R-41) |
| `STORAGE_ENDPOINT` | compose가 `http://seaweedfs:8333` 주입 | | AWS면 빈 값 |
| `STORAGE_PUBLIC_ENDPOINT` | `http://localhost:8333` | | AWS면 빈 값 |
| `STORAGE_PUBLIC_BASE_URL` | `http://localhost:8333/findear-images` | | |
| `STORAGE_BUCKET` | `findear-images` | | storage-init도 사용 |
| `STORAGE_PATH_STYLE` | `true` | | AWS면 `false` |
| `STORAGE_PRESIGN_EXPIRE_SECONDS` | `600` | | |
| `AWS_REGION` | `ap-northeast-2` | | |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | (SeaweedFS용 임의값) | O | seaweedfs, storage-init도 사용. EC2에선 비움(IAM Role) |

### batch
| 변수 | `.env.example` 값 | 비밀 | 비고 |
|---|---|---|---|
| `LOST112_SERVICE_KEY` | (발급, Decoding 키) | O | U-05 |
| `LOST112_BASE_URL` | `https://apis.data.go.kr/1320000` | | |
| `LOST112_COLLECT_ENABLED` | `false` | | 키 발급 후 `true` |
| `LOST112_COLLECT_DAYS` | `30` | | 최근 N일 수집 (O-4) |
| `LOST112_PAGE_SIZE` | `1000` | | |
| `BATCH_SCHEDULING_ENABLED` | `true` | | 테스트 시 `false` |
| `POLICE_JOB_CRON` | `0 0 4 * * *` | | Lost112 수집(옵션) + Lost112 매칭 |
| `FINDEAR_JOB_CRON` | `0 0 */2 * * *` | | Findear 매칭 |

### match (mock)
| 변수 | `.env.example` 값 | 비고 |
|---|---|---|
| `MATCH_MOCK_SEED` | `42` | 결정적 의사난수 시드 |
| `MATCH_MOCK_LATENCY_MS` | `0` | AI 응답 지연 흉내 (모니터링 확인용) |
| `MATCH_MOCK_MAX_RESULTS` | `100` | 팀 시절과 동일 상한 |

### 모니터링
| 변수 | `.env.example` 값 | 비밀 |
|---|---|---|
| `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` | `admin` / (임의) | O |
| `PROMETHEUS_RETENTION` | `7d` | |

### 배포 전용 (`compose.prod.yml`)
| 변수 | 예시 | 비고 |
|---|---|---|
| `IMAGE_REGISTRY` | `ghcr.io/ehighg` | GHCR 이름은 소문자 |
| `IMAGE_TAG` | `latest` 또는 커밋 SHA | 롤백 시 이전 SHA |
| `MAIN_HOST_PORT` | `80` | 로컬 기본값은 `8080` (위 "로컬 호스트 포트") |

## 7. 설정 파일 구조 (각 Spring 앱)

| 파일 | 내용 |
|---|---|
| `application.yml` | 공통. 모든 값은 `${ENV:기본값}` 형태, 비밀값은 기본값 없음 |
| `application-local.yml` | 로컬 전용: 개발용 엔드포인트 활성(D-26), p6spy·SQL 로그, localhost 기본값 |
| `application-prod.yml` | 배포 전용: 개발용 엔드포인트 비활성, 로그 레벨, 보안 설정 |

- match(mock)는 local·prod 차이가 없어 `application.yml` 하나만 둔다 (R-40). compose가 넘기는 `SPRING_PROFILES_ACTIVE`는 영향이 없다.

- `spring.profiles.active: secret` 방식과 `application-secret.yml`은 폐기합니다.
- 비밀 파일(`secrets/*.json`)은 경로만 설정에 두고 파일은 마운트합니다.

## 8. 시드·더미 데이터

- **개발용 소량 시드** (R-11b, D-48): `infra/db/seed/R__dev_seed.sql` — Flyway **반복 마이그레이션**. `compose.override.yml`(로컬 전용)만 flyway에 이 폴더를 마운트하고 `FLYWAY_LOCATIONS`에 추가하므로 배포(`compose.yml` + `compose.prod.yml`)에는 들어가지 않는다. 내용: 기관 1(서울역 유실물센터), 회원 2(NORMAL `010-0000-0001` / MANAGER `010-0000-0002`, 테스트 로그인용), 분실물 2(board 1·2), 습득물 2(board 3·4). 고정 PK + `INSERT … AS new_row ON DUPLICATE KEY UPDATE`라 다시 실행해도 결과가 같고, `SET NAMES utf8mb4`가 있어 mysql 클라이언트로 직접 실행해도 된다. 파일을 고치면 Flyway가 다시 적용하며, 고정 PK 1~4번 행을 덮어쓴다 (빈 DB 전제). 이미지 컬럼이 바뀌는 R-24 등 스키마가 바뀌면 시드도 같이 고친다.
  - 주의: 시드가 적용된 로컬 볼륨에 `compose.override.yml` 없이(`-f compose.yml`만) flyway를 실행하면 Flyway가 "적용됐지만 파일이 없는" 반복 마이그레이션으로 보고 검증에 실패한다. 로컬에서는 항상 기본(`docker compose …`)으로 실행하고, 배포 경로를 시험할 땐 `down -v`로 볼륨을 비운다.
- **대량 더미**: `infra/db/dummy/*.sql` (R-02에서 `exec/data/mainDB/`에서 이동). 회원 2만, 습득물 100만, 분실물 500만 등 성능 실험용. MySQL 전용 문법. stub 전용 `batchDB_RDB-version/*`은 삭제함. 1차 검증 시나리오에서는 쓰지 않음. 스크립트마다 `use findear;`, `set foreign_key_checks = 0;`으로 시작함. 쓸 때 주의: `dummyScript_Agency.sql`의 `insert into tbl_Agency`는 테이블명 대소문자를 구분하는 Linux MySQL(컨테이너 기본값)에서 실패하므로 `tbl_agency`로 고쳐서 실행.
- **Lost112 데이터**: batch 수집으로 채움. 키 발급(U-05)은 1차 작업 이후로 미뤄졌으므로(D-37) **샘플 문서 적재 스크립트를 만든다** (`infra/elasticsearch/seed/`, R-32). 샘플은 공공데이터포털 명세서의 응답 예시 형식을 따르고, 실제 수집 데이터는 커밋하지 않는다.
