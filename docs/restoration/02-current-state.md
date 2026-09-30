# 02. 현재 레포 상태 (master `d4f6025` 기준, 2026-09-29 조사)

> 복구 작업을 시작하는 시점의 기준선입니다. 작업이 진행되면 이 문서가 아니라 [08-work-plan.md](08-work-plan.md)와 [10-worklog.md](10-worklog.md)에 변경을 기록합니다.
>
> **Phase 0(2026-09-30) 이후 달라진 경로·브랜치** — 아래 본문은 기준선 시점 그대로입니다.
> - 팀 batch: `old-servers/batch/` → **`batch/`**. stub `batch/`, `old-servers/match/`, `config/`, `infra/findear-infra-setting/`, `infra/git-settings/`, `infra/README.md`, `.gitlab/`은 삭제 (원본은 `old-master`)
> - `exec/`: 문서 → `docs/legacy/`, 더미 스크립트(`mainDB`) → `infra/db/dummy/`, `batchDB_RDB-version/` 삭제
> - 브랜치 `Chore/10-reset_env`, `claude/happy-babbage-qt991n` 삭제. 루트 `.gitignore`·`.gitattributes` 추가, `gradlew` 실행 권한 부여, K-08 해결

## 1. 디렉토리별 현황과 처리 계획

| 경로 | 현재 내용 | 팀 종료 시점 대비 | 복구 계획 (작업 ID) |
|---|---|---|---|
| `main/` | 메인 API (Spring Boot 3.2.3, Java 17). 개인 리팩토링 반영본 | 서비스 추상화, Querydsl, 인덱스·기본값 추가, 서버 URL 설정화 | **유지·업그레이드** (Phase 2) |
| `batch/` | 개인 리팩토링 때 만든 **stub** batch (Boot 3.3.4, MySQL `findear_batchdb`, 더미 데이터 기반) | 팀 batch와 다른 코드 | **삭제** (R-02) |
| `old-servers/batch/` | **팀 batch 원본** (`2af1413:batch`와 완전히 동일) | 동일 | `batch/`로 이동 후 Boot 3.5 마이그레이션 (R-02, Phase 3) |
| `old-servers/match/` | 팀 match(Django + AI) 원본 (`2af1413:match`와 동일) | 동일 | **삭제**, Spring Boot mock으로 대체 (R-02, R-40). API 계약은 [07](07-api-contracts.md)에 보존 |
| `config/` | Spring Cloud Config Server (`2af1413:config`와 동일) | 동일 | **삭제** (R-02, D-07) |
| `front/` | React PWA (`2af1413:front`와 **완전히 동일**, 리팩토링 없음) | 동일 | 1차 범위 외. 손대지 않음 (P3) |
| `exec/` | `포팅 매뉴얼.md`, `서비스 시연 시나리오.pdf`, `data/`(더미 데이터 스크립트) | 팀 DDL `Dump20240403.sql`은 삭제됨, 더미 스크립트 추가 | 문서는 `docs/legacy/`로, 더미 스크립트는 `infra/db/dummy/`로 이동 (R-02) |
| `infra/findear-infra-setting/` | 팀 인프라 스냅샷 (compose, Dockerfile, nginx, 스크립트) — **비밀값 포함** | 동일 | **삭제** (R-02). 내용은 [01](01-legacy-inventory.md)에 정리, 원본은 `old-master` |
| `infra/git-settings/` | Jira 이슈키 자동 삽입 git 훅 (`S10P22A706-`) | 동일 | 삭제 (R-02, 현 워크플로와 무관) |
| `.gitlab/` | GitLab MR 템플릿 | 동일 | 삭제 (R-02) |
| `.github/ISSUE_TEMPLATE/` | 버그 리포트, 단순 이슈 템플릿 | 신규 | 유지 |
| 루트 | `.gitignore`·`.gitattributes` 없음. 모든 `gradlew`가 실행 권한 없음(100644) | – | 루트 `.gitignore`/`.gitattributes` 추가, `gradlew` +x (R-02) |

## 2. main 모듈 상세

### 빌드 설정
- Spring Boot 3.2.3, `io.spring.dependency-management` 1.1.4, Gradle wrapper 8.5, `sourceCompatibility = '17'`
- 의존성: data-jpa, data-redis, webflux(WebClient 용), **mail(미사용)**, security, web, actuator, p6spy 1.9.0, jjwt 0.11.5, lombok, **mariadb + mysql 드라이버 둘 다**, Querydsl 5.0.0(jakarta) + blaze-persistence querydsl expressions 1.6.11, firebase-admin 7.1.1, mockito
- Spring Cloud Config client는 이미 제거됨

### 설정 (`main/src/main/resources/application.yml`, 커밋돼 있음)
- `spring.profiles.active: secret` → `application-secret.yml`(git 제외, **로컬 전용이었고 유실**)에서 `${RDB-url}`, `${RDB-username}`, `${RDB-password}`, `${naver-client-id}`, `${naver-client-secret}`, `${jwt-secret}`를 채우는 구조
- datasource 드라이버 `com.mysql.cj.jdbc.Driver`, `ddl-auto: validate` (→ 스키마가 미리 있어야 기동)
- `servers.batch-server.url`, `servers.match-server.url` 모두 `http://localhost:8081` (stub batch가 match 역할까지 겸함)
- Redis `localhost:6379`, Naver redirect `http://localhost:8080/members/login`

### 코드 구조
- 패키지: `Alarm`(SSE·FCM·알림), `board`(command/query, 분실물·습득물·Lost112 스크랩), `matching`, `member`(command/query, Naver OAuth), `message`(쪽지), `security`(JWT), `common`
- 컨트롤러 엔드포인트 47개 — 목록은 [07-api-contracts.md](07-api-contracts.md#4-main-외부-api-목록)
- JPA 엔티티 13개 (팀 시절 테이블과 동일한 이름). 리팩토링 변경점:
  - `tbl_board` 인덱스 `ix_is_lost_delete_yn(is_lost, delete_yn)`, `delete_yn` 기본값 0
  - `tbl_lost_board` 인덱스 `ix_lost_at_board_id(lost_at, board_id)`
  - `tbl_member`: `password` 컬럼 삭제, `withdrawal_yn` 기본값 0
- 테스트: `LostBoardQueryServiceTest`(Mockito 단위 테스트 4개), `MainApplicationTests`(contextLoads — DB 필요)

### 빌드 확인 결과 (2026-09-29, 클라우드 세션)
- JDK 21 + Gradle 8.5: `compileJava`, `compileTestJava` **성공**
- `LostBoardQueryServiceTest` **4/4 통과**
- `MainApplicationTests`는 DB가 없어 실행하지 않음
- 경고: Lombok `@Builder will ignore the initializing expression` 다수 (Member, Board, AcquiredBoard, Agency, AcquiredBoardDto, BoardDto) — 과거 "기본값 미설정" 버그들과 같은 원인
- 참고: 이 환경에서 Maven Central이 일시적으로 429를 반환함 → 잠시 후 `--max-workers=1`로 재시도하면 됨

### 알려진 문제 (main)

| ID | 문제 | 위치 | 조치 작업 |
|---|---|---|---|
| K-01 | Lost112 목록·총개수 조회가 batch의 `/search`, `/search/total`이 아니라 루트(`?page=`), `/total`로 호출됨. 팀 코드는 base URL이 `…/batch/search`였는데 `415d73e`(2024-11-14)에서 설정값으로 바꾸며 `/search`가 빠짐 | `AcquiredBoardQueryServiceImpl` | R-22 |
| K-02 | main이 호출하는 `POST {batch}/findear/matching`이 stub batch에는 없음 → master 조합은 원래도 끝까지 동작하지 않음 (팀 batch 복원 시 해결) | `LostBoardCommandServiceImpl` | R-30~R-35 |
| K-03 | FCM 비활성: `FCMInitializer`의 `@PostConstruct` 주석 처리, 키 파일 경로 하드코딩 (`key/findear-bfd63-…json`) | `Alarm/service/FCMInitializer` | R-23 |
| K-04 | VWorld API 키 하드코딩 | `common/utils/query/LocationController` | R-21 |
| K-05 | CORS 허용 origin 하드코딩 (`https://j10a706.p.ssafy.io`, `localhost:5173/4173`) | `common/config/WebConfig` | R-21 |
| K-06 | 개발용 백도어: `POST /members/login`이 **전화번호만으로 로그인** (비밀번호 검증 주석), `POST /members`(가입) permitAll | `MemberCommandController`, `MemberCommandServiceImpl.localLogin` | R-27 (local 프로필 한정) |
| K-07 | `/alarm/**` 전체 permitAll → `POST /alarm/send-fcm/{memberId}`, `/alarm/send-data/{memberId}` 테스트 엔드포인트를 누구나 호출 가능, SSE 구독도 타인 ID로 가능 | `SecurityConfig`, `AlarmController` | R-27 |
| K-08 | `main/.gitignore`에 `!**/src/main/resources/key/` 예외 → FCM 키 폴더가 **커밋 가능한 상태** | `main/.gitignore` | R-02 |
| K-09 | `WebClient.builder()`를 직접 생성해 사용 → HTTP client 메트릭 미수집 (RestTemplate은 빈으로 주입) | `AcquiredBoardCommandServiceImpl`, `LostBoardCommandServiceImpl` | R-50 |
| K-10 | `httpBasic` 활성, mail 의존성 미사용, mariadb 드라이버 불필요 | `SecurityConfig`, `build.gradle` | R-20 |
| K-11 | SSE emitter가 메모리(Map)에 저장 → 단일 인스턴스 전제 (이번 구성은 단일 인스턴스라 유지) | `EmitterRepository` | – |

## 3. 팀 batch (`old-servers/batch/`) — 복원 대상

- Spring Boot 2.7.12, Spring Cloud 2021.0.8(config client), Java 17, Gradle 8.6
- 패키지: `police`(Lost112 수집·검색, ES `police_acquired_data`), `ours`(Findear 매칭, Lost112 매칭, 매칭 로그), `alarm`(주석 처리된 FCM), `common`
- API 목록과 계약은 [07-api-contracts.md](07-api-contracts.md#2-main--batch)

### Boot 3.5 / ES 8 마이그레이션 지점
| 항목 | 현재 | 조치 |
|---|---|---|
| javax | `javax.persistence.*` 6곳, `javax.transaction.Transactional` 4곳 (`javax.xml.*`은 JDK라 유지) | jakarta로 변경 |
| Spring Batch 4 API | `JobBuilderFactory`, `StepBuilderFactory` (`PoliceJobConfig`, `FindearJobConfig`), `new JobParameter(...)` 4곳, `@EnableBatchProcessing` | Batch 5: `JobBuilder`/`StepBuilder` + `JobRepository` + `PlatformTransactionManager`, `JobParametersBuilder`. Boot 3에선 `@EnableBatchProcessing`을 붙이면 자동설정이 꺼지므로 제거 |
| ES 클라이언트 | `RestHighLevelClient` (ES 7 전용, 제거됨) — `ElasticSearchConfig`, `PoliceAcquiredDataService`, `PoliceDataService`, `FindearDataService` | Spring Data Elasticsearch 5.5(`ElasticsearchOperations`, `NativeQuery`/`CriteriaQuery`, search_after) + `spring.elasticsearch.uris` |
| 하드코딩 URL | Lost112 API 4곳(http), match URL 4곳(`https://j10a706…`) | 설정값으로, Lost112는 https |
| HTTP 클라이언트 | `new RestTemplate()` 3곳 | `RestClient`/`RestTemplateBuilder` 빈 사용 (메트릭 수집) |
| ES 문서 ID | 매칭 로그 ID를 `repository.count() + 1`로 부여 → 동시 실행 시 충돌·덮어쓰기 | 결정적 ID (`{lostBoardId}-{acquiredBoardId}` 등) |
| Lost112 수집 | 모든 페이지 응답을 메모리에 모은 뒤 파싱, 인덱스 전체 삭제 후 재적재 | 페이지 단위 파싱·bulk 인덱싱, 수집 기간 설정화 |
| 위험 엔드포인트 | `DELETE /search`, `DELETE /findear`, `DELETE /police`(전체 삭제), `/…/test-api`, `/search/test` | local 프로필 한정 또는 제거 |
| Batch 메타 테이블 | `initialize-schema: never` (당시 테이블 생성 방법 기록 없음) | Flyway에 Spring Batch 5.2 MySQL 스키마 포함 |

## 4. stub batch (`batch/`) — 삭제 예정

- 개인 리팩토링 때 main 개발용으로 만든 대체 서버. Lost112 습득물·매칭 결과를 MySQL(`findear_batchdb`)의 `tbl_lost112_acquired`, `tbl_findear_matching`, `tbl_lost112_matching`에 두고 조회만 함. `/process`(AI 자동채움) stub 포함.
- 스케줄러·ES·Lost112 API 호출 없음. 목록 조회 엔드포인트(`GET /search`)가 없음.
- 응답 형태(`matchingList`, `totalCount`)는 팀 batch와 같음.

## 5. 기타

- `exec/data/`: MySQL 전용 문법(`cte_max_recursion_depth`)의 대량 더미 데이터 스크립트 (회원 2만, 습득물 100만, 분실물 500만 등 — Querydsl 성능 실험용). `mainDB/*`는 DB `findear`, `batchDB_RDB-version/*`은 stub용 `findear_batchdb`(ES 사용 결정으로 불필요).
- `front/`: `front/.env`(값은 비어 있음), `src/Firebase.ts`·`public/firebase-messaging-sw.js`에 옛 Firebase 웹 설정, `index.html`에 Kakao 키, 도메인 `j10a706` 하드코딩. 1차 범위 외.

## 6. 브랜치

| 브랜치 | 내용 | 처리 |
|---|---|---|
| `master` | 개인 리팩토링 결과 (`d4f6025`) | 복구 작업의 기준 |
| `old-master` | 팀 종료 시점 `2af1413` | **보존 (삭제·수정 금지)** — 삭제하는 레거시의 원본 |
| `Chore/10-reset_env` | `76edc42` (2025-09-28), master 미병합. 기존 main/batch/config/match를 `old-servers/`로 옮기고 main을 새 뼈대(Boot 3.5.6, ES starter, REST Docs)로 교체, `infra/config_new/docker-compose.yml` 초안(main/batch/match + MariaDB 10.11.7, Redis 7.2.4, ES 8.12.2 security off), `front/.env`에 Naver Client ID/Secret 커밋 | **삭제 (R-01)**. 쓸 만한 내용은 이 표로 보존됨. Naver 키 재발급 필요(U-01) |
| `feature/*`, `fix/*`, `test/*` (13개) | 개인 리팩토링 세부 작업 기록 (README 규칙: 원격 브랜치에 세부 내역 보존) | 유지 |
| `feature/reset-env` | 개인 리팩토링 초기(2024-08~11) 작업 흐름, `63ba032`에 MariaDB 10.11.8용 DDL 스크립트가 있었음 | 유지 |
| `claude/happy-babbage-qt991n` | 이 문서들이 처음 작성된 세션 브랜치 | master 병합 후 정리 |
