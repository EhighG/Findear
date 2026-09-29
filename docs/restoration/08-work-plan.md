# 08. 작업 계획 · 진행 트래커

> 사용법: 작업은 R-xx 단위. 시작하면 상태를 `진행중`, 끝나면 `[x]` + `완료(날짜)`로 바꾸고 [10-worklog.md](10-worklog.md)에 기록.
> 레포 규칙: 이슈 생성 → 브랜치 `{feature|fix|test}/{이슈번호}-{이름}` → master 병합 (Claude Code web 세션은 세션 지정 브랜치 사용). 커밋 `Type: 한국어 설명`.
> 순서: Phase 0 → 1 → (2 ∥ 4) → 3 → 5 → 6. 사용자 작업(U-xx)은 병렬로 미리 진행.

## 사용자 작업 (코드 밖)

| ID | 작업 | 필요 시점 | 상태 |
|---|---|---|---|
| U-01 | Naver Developers에서 Client Secret **재발급** (`Chore/10-reset_env` 커밋 노출분) | 즉시 | [ ] |
| U-02 | GitHub 설정 → Code security → Secret scanning + Push protection 활성화 | 즉시 | [ ] |
| U-03 | (포크 레포) GitHub Actions 탭에서 워크플로 활성화 | R-60 | [ ] |
| U-04 | Firebase 새 프로젝트, 서비스계정 JSON, 웹앱 설정, VAPID 키 ([05 §2](05-external-integrations.md#2-firebase-cloud-messaging-웹푸시)) | R-23 | [ ] |
| U-05 | data.go.kr Lost112 API 2종 활용신청 + 트래픽 한도 확인 ([05 §3](05-external-integrations.md#3-공공데이터포털-lost112-api)) | R-32 (승인 대기 있으니 미리) | [ ] |
| U-06 | Naver 로그인 앱 등록 (callback `http://localhost:8080/members/login`, 테스트 ID) | R-25 | [ ] |
| U-07 | VWorld 인증키 발급 | R-26 | [ ] |
| U-08 | (유료, 배포 시에만) AWS 계정·EC2·S3 — `infra/aws/README.md` 절차 | 배포 시 | [ ] |
| U-09 | 이 문서 브랜치를 master에 병합 (다른 세션이 문서를 보도록) | 즉시 | [ ] |

## Phase 0 — 정리

- [ ] **R-01** `Chore/10-reset_env` 원격 브랜치 삭제 (`git push origin --delete Chore/10-reset_env`). 내용은 [02 §6](02-current-state.md#6-브랜치)에 보존됨.
- [ ] **R-02** 레거시 정리 (D-22)
  - 삭제: `config/`, stub `batch/`, `old-servers/match/`, `infra/findear-infra-setting/`, `infra/git-settings/`, `.gitlab/`
  - 이동: `old-servers/batch/` → `batch/`, `exec/포팅 매뉴얼.md`·`exec/서비스 시연 시나리오.pdf` → `docs/legacy/`, `exec/data/mainDB/*` → `infra/db/dummy/` (`batchDB_RDB-version/` 삭제)
  - 루트 `.gitignore`(`.env`, `!.env.example`, `secrets/`, `**/build/`, `.gradle/`, `.idea/`, `*.iml`, `.DS_Store`, `tools/fcm-test/firebase-config.js`), `.gitattributes`(`*.sh`·`gradlew` LF)
  - `git update-index --chmod=+x main/gradlew batch/gradlew`
  - `main/.gitignore`의 `!**/src/main/resources/key/` 예외 제거 (K-08)
  - 완료 기준: 트리가 [04 §7](04-target-architecture.md#7-목표-디렉토리-구조)와 일치(아직 없는 폴더 제외), `git grep -nE 'sk-[A-Za-z0-9]{20}|BEGIN PRIVATE KEY'` 결과 없음, main 컴파일 성공

## Phase 1 — 인프라 골격

- [ ] **R-10** `compose.yml` / `compose.override.yml` / `.env.example`: mysql, redis, elasticsearch, seaweedfs, 볼륨·네트워크·헬스체크·메모리 제한([04](04-target-architecture.md)), 환경변수([06 §6](06-db-and-config.md#6-환경변수-전체-목록)). 완료 기준: `docker compose up -d` → 전부 healthy, `down` 후 재기동해도 데이터 유지
- [ ] **R-11** Flyway: `flyway` one-shot 서비스, `V1__init_schema.sql`(master 엔티티 기준, [06 §2](06-db-and-config.md#2-스키마-관리-flyway-d-20)), `V2__spring_batch_schema.sql`, 개발용 소량 시드 `infra/db/seed/`. 완료 기준: flyway 성공 종료, main이 `ddl-auto: validate`로 기동 *(V1은 R-20 이후 Boot 3.5 Hibernate로 생성 — R-20과 함께 진행 가능)*
- [ ] **R-12** `infra/mysql/initdb/`: exporter 계정 생성 스크립트, 문자셋·시간대 설정
- [ ] **R-13** SeaweedFS: `s3.json` 템플릿 + entrypoint(환경변수 렌더링), anonymous Read, `storage-init`(aws-cli로 버킷·CORS). 완료 기준: aws-cli presigned PUT 업로드 성공, 공개 URL로 GET 성공
- [ ] **R-14** 모니터링 인프라: prometheus(`infra/monitoring/prometheus/prometheus.yml`), grafana provisioning, cadvisor, mysqld/redis/es exporter, profile `monitoring`. 완료 기준: 인프라 타깃 UP, cAdvisor가 Docker Desktop에서 동작하는지 확인·기록

## Phase 2 — main 복구

- [ ] **R-20** 빌드 정비: Boot 3.2.3 → 3.5.x, mail·mariadb 의존성·`httpBasic` 제거, firebase-admin 9.x, Querydsl 5.1.0, `micrometer-registry-prometheus` 추가, 멀티스테이지 Dockerfile(런타임에 curl). 완료 기준: 빌드·단위테스트 통과, 이미지 빌드 성공
- [ ] **R-21** 설정 외부화: `application.yml`/`-local`/`-prod`([06 §7](06-db-and-config.md#7-설정-파일-구조-각-spring-앱)), `profiles.active: secret` 폐기, VWorld 키·CORS origin·서버 URL 환경변수화, 관리 포트 8081. 완료 기준: `git grep -n j10a706 main/` 0건, 하드코딩 키 0건
- [ ] **R-22** 버그 수정: K-01(Lost112 목록·총개수 경로 `/search`), Lombok `@Builder.Default` 경고 정리
- [ ] **R-23** FCM 복구: `fcm.enabled` 조건부 초기화, `FCM_CREDENTIALS_PATH`, `secrets/` 마운트 (K-03). 완료 기준: 키 없이도 기동, R-80 테스트 페이지로 알림 수신
- [ ] **R-24** 스토리지: AWS SDK v2 `S3Client`(내부 엔드포인트) + `S3Presigner`(공개 엔드포인트), `POST /images/presign`, 게시글 등록 시 object key 저장·URL 조립 (D-13). 완료 기준: curl로 presign → PUT → 게시글 등록 → 조회 응답에 이미지 URL
- [ ] **R-25** Naver 로그인 검증 (U-06 후): [05 §4](05-external-integrations.md#4-naver-로그인) 절차대로 JWT 발급 확인
- [ ] **R-26** VWorld 검증 (U-07 후): `/location/search`, `/location/address`
- [ ] **R-27** 보안 정리 (D-26): K-06(전화번호 로그인·테스트 가입), K-07(`/alarm/send-*`)을 local 프로필 한정, SSE 구독은 본인 ID만. (선택) Redis key serializer 개선, Testcontainers로 `MainApplicationTests` 복구

## Phase 3 — batch 복구

- [ ] **R-30** Boot 3.5 마이그레이션 ([02 §3](02-current-state.md#3-팀-batch-old-serversbatch--복원-대상)): jakarta, Spring Batch 5 API, `@EnableBatchProcessing` 제거, `RestHighLevelClient` → Spring Data ES 5.5(`ElasticsearchOperations`), Spring Cloud 제거, 멀티스테이지 Dockerfile. 완료 기준: compose의 MySQL·ES에 붙어 기동, `validate` 통과
- [ ] **R-31** 설정 외부화: match URL, Lost112 키·URL(https), cron·on/off, 관리 포트 8083 ([06 §6](06-db-and-config.md#batch))
- [ ] **R-32** Lost112 수집 개선: 키 URL 인코딩, 페이지 단위 파싱 + bulk 인덱싱(768MB 제한 내 동작), 수집 기간 설정, `atcId` 문서 ID. 완료 기준: 최근 N일 수집 성공, `GET /search/total` > 0
- [ ] **R-33** ES 매핑 명시([06 §3](06-db-and-config.md#3-elasticsearch-d-06)), 매칭 로그 결정적 ID
- [ ] **R-34** 잡·스케줄 복원: `policeJob`(수집 on/off + Lost112 매칭), `findearJob`, 수동 트리거 유지. 완료 기준: 짧은 cron으로 두 잡 실행 → 매칭 로그 적재
- [ ] **R-35** main↔batch 계약 검증 ([07 §2](07-api-contracts.md#2-main--batch)): 분실물 등록 → 매칭 → 매칭 목록 조회, Lost112 목록·스크랩 end-to-end
- [ ] **R-36** 정리: 주석 처리된 FCM·alarm 코드 삭제, 위험 엔드포인트 local 한정 ([07 §3](07-api-contracts.md#3-batch-api-전체-팀-버전와-1차-처리)), `new RestTemplate()` → 빈

## Phase 4 — match mock (Phase 2와 병렬 가능)

- [ ] **R-40** `match/` 신규 Spring Boot 3.5 앱: [07 §5](07-api-contracts.md#5-match-mock-동작-명세-r-40-d-28) 명세대로 3개 API, `MatchingScorer` 인터페이스, 관리 포트 8085, Dockerfile. 완료 기준: JSON 픽스처 계약 테스트 통과, batch·main과 연동 동작

## Phase 5 — 모니터링 연결

- [ ] **R-50** 앱 지표: 3개 앱 actuator/prometheus, `application` 태그, WebClient·RestTemplate을 Builder 빈으로(K-09), 커스텀 지표([04 §6](04-target-architecture.md#6-모니터링-설계-d-18))
- [ ] **R-51** Grafana 대시보드: 후보 ID 대시보드 JSON 커밋 + "Findear Overview" 작성. 완료 기준: 모든 Prometheus 타깃 UP, 패널에 데이터 표시. 실측 메모리로 [04 §5](04-target-architecture.md#5-리소스-산정-메모리) 표 갱신

## Phase 6 — 배포 준비 (P5, [09](09-deploy-and-aws.md))

- [ ] **R-60** `.github/workflows/ci.yml`: PR·push 시 main/batch/match 빌드·테스트 (U-03)
- [ ] **R-61** `.github/workflows/images.yml`: master push 시 GHCR 이미지 빌드·푸시(`ghcr.io/ehighg/findear-{main,batch,match}`, 태그 `sha`·`latest`). 첫 푸시 후 패키지 visibility public 확인
- [ ] **R-62** `compose.prod.yml`: GHCR 이미지, main `80:8080`, Redis·ES 비밀번호/보안 on, 모니터링 127.0.0.1 바인딩, node-exporter, `restart`, 로그 로테이션
- [ ] **R-63** `infra/deploy/init-host.sh`(Ubuntu: Docker, `vm.max_map_count`, swap), `deploy.sh`(pull → up, `IMAGE_TAG` 롤백)
- [ ] **R-64** AWS S3 연동 키트 `infra/aws/` ([09 §4](09-deploy-and-aws.md#4-aws-s3-연동-키트))
- [ ] **R-65** (선택) `deploy.yml`: workflow_dispatch로 SSH 배포 (시크릿 이름만 문서화)

## Phase 7 — 검증 도구

- [ ] **R-80** `tools/fcm-test/`: `index.html` + `firebase-messaging-sw.js` + `firebase-config.example.js`, `python3 -m http.server 5500 -d tools/fcm-test`로 실행. 흐름: 테스트 로그인으로 JWT → 알림 권한 → `getToken(VAPID)` → `POST /notification/new` → `POST /alarm/send-fcm/{memberId}`

## 1차 목표 이후 (기록만)
- 프론트 재구축 (P3): 기능 유지·디자인 전면 수정, presigned 업로드·Naver(`client_secret` 제외)·FCM·SSE 계약 반영, O-5 결정
- HTTPS 재검토 (O-7), 알림(Alerting)·로그 수집(Loki), Java 21·Boot 4 전환
