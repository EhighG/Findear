# 08. 작업 계획 · 진행 트래커

> 사용법: 작업은 R-xx 단위. 시작하면 상태를 `진행중`, 끝나면 `[x]` + `완료(날짜)`로 바꾸고 [10-worklog.md](10-worklog.md)에 기록.
> 순서: Phase 0 → 1 → (2 ∥ 4) → 3 → 5 → 6 → 7 → 8(최종 검증). **세션은 Phase 단위**로 진행하고, Phase가 끝나면 멈춰서 사용자에게 보고한다 (D-35).
>
> **진행 절차 (D-33, D-34)**
> 1. Phase 착수 시 GitHub 이슈 1개 생성 (상위 이슈의 sub-issue). 템플릿은 `.github/ISSUE_TEMPLATE/simple-issue-template.md`, 작업 목록에 그 Phase의 R-xx를 체크박스로.
> 2. R-xx마다 master에서 브랜치 `{feature|fix|test}/{Phase 이슈번호}-{이름}` 생성. 커밋 `Type: 한국어 설명` (이 단계에선 이슈번호 없음).
> 3. 완료 기준 통과 → 비밀값 검사(아래) → 작업 브랜치 원격 push.
> 4. 로컬에서 `git rebase -x`/`--exec` 등으로 각 커밋 본문 끝에 `Related to #{Phase 이슈번호}` 추가 → master에 fast-forward 병합 → `git push origin master` → 로컬 브랜치 삭제.
> 5. Phase의 R-xx가 모두 끝나면 이슈 작업 목록 체크 후 이슈 닫기, 문서 갱신, 사용자에게 보고.
>
> **비밀값 검사 (push 전 매번, D-37)**: `git diff --cached`/`git diff origin/master...HEAD`를 눈으로 확인하고, `git grep -nIE 'sk-[A-Za-z0-9]{20}|BEGIN (RSA |EC )?PRIVATE KEY|AKIA[0-9A-Z]{16}|"private_key"|client_secret\s*[:=]\s*[A-Za-z0-9_-]{8}' -- ':!docs/restoration/'` 결과가 없어야 한다 (이 문서 자체가 패턴 문자열을 담고 있어 `docs/restoration/`은 제외). `.env`, `secrets/`, `firebase-config.js`가 추적되지 않는지 `git ls-files`로 확인.
>
> **원본 레포 금지 (D-36)**: `2TF4/findear`에는 어떤 쓰기도 하지 않는다. gh 대상은 `.claude/settings.json`의 `GH_REPO=EhighG/Findear`로 고정돼 있다.
>
> **전체 동시 기동 금지 (D-32)**: 1차 작업에서는 compose 전체를 한 번에 띄우지 않고 자원도 실측하지 않는다. 전체 구성은 `docker compose config`로 검증, 동작 확인은 R-xx에 필요한 서비스만 부분 기동 후 `docker compose down`.

## 사용자 작업 (코드 밖)

| ID | 작업 | 필요 시점 | 상태 |
|---|---|---|---|
| U-01 | Naver Developers에서 Client Secret **재발급** (`Chore/10-reset_env` 커밋 노출분) | 1차 작업 완료 후 (D-37) | [ ] |
| U-02 | GitHub 설정 → Code security → Secret scanning + Push protection 활성화 | 1차 작업 완료 후 (D-37). 그동안 Claude가 push 전 비밀값 검사 | [ ] |
| U-03 | (포크 레포) GitHub Actions 탭에서 워크플로 활성화 | R-60 | [ ] |
| U-04 | Firebase 새 프로젝트, 서비스계정 JSON, 웹앱 설정, VAPID 키 ([05 §2](05-external-integrations.md#2-firebase-cloud-messaging-웹푸시)) | R-23 | [ ] |
| U-05 | data.go.kr Lost112 API 2종 활용신청 + 트래픽 한도 확인 ([05 §3](05-external-integrations.md#3-공공데이터포털-lost112-api)) | 1차 작업 완료 후 (D-37). R-32는 샘플 데이터로 진행 | [ ] |
| U-06 | Naver 로그인 앱 등록 (callback `http://localhost:8080/members/login`, 테스트 ID) | R-25 | [ ] |
| U-07 | VWorld 인증키 발급 | R-26 | [ ] |
| U-08 | (유료, 배포 시에만) AWS 계정·EC2·S3 — `infra/aws/README.md` 절차 | 배포 시 | [ ] |
| ~~U-09~~ | ~~이 문서 브랜치를 master에 병합~~ → Claude가 R-00에서 수행 (D-34) | – | – |

## Phase 0 — 정리

- [ ] **R-00** 작업 준비
  - 이슈 #11(2025-09 "개발환경 다시 세팅")에 "새 이슈로 대체" 코멘트 후 닫기
  - 상위 이슈 "Findear 복구 1차" 생성(본문: README 링크, Phase 목록), Phase 0 이슈 생성 후 sub-issue로 연결
  - gh 안전장치 확인 (D-36): `echo $GH_REPO` = `EhighG/Findear`, `gh repo set-default --view` = `EhighG/Findear`
  - 문서 브랜치(`claude/happy-babbage-qt991n`)의 커밋 3개에 `Related to #{상위 이슈}`를 붙여 master에 병합·push (구 U-09). 원격 `claude/happy-babbage-qt991n`은 병합 후 삭제 ([02 §6](02-current-state.md#6-브랜치) 계획)
  - 완료 기준: master에 `docs/restoration/`, `CLAUDE.md`, `.claude/settings.json` 존재, #11 닫힘
- [ ] **R-01** `Chore/10-reset_env` 원격 브랜치 삭제 (`git push origin --delete Chore/10-reset_env`). 내용은 [02 §6](02-current-state.md#6-브랜치)에 보존됨. 사용자 승인(2026-09-29).
- [ ] **R-02** 레거시 정리 (D-22)
  - 삭제: `config/`, stub `batch/`, `old-servers/match/`, `infra/findear-infra-setting/`, `infra/git-settings/`, `.gitlab/`
  - 이동: `old-servers/batch/` → `batch/`, `exec/포팅 매뉴얼.md`·`exec/서비스 시연 시나리오.pdf` → `docs/legacy/`, `exec/data/mainDB/*` → `infra/db/dummy/` (`batchDB_RDB-version/` 삭제)
  - 루트 `.gitignore`(`.env`, `!.env.example`, `secrets/`, `**/build/`, `.gradle/`, `.idea/`, `*.iml`, `.DS_Store`, `tools/fcm-test/firebase-config.js`), `.gitattributes`(`*.sh`·`gradlew` LF)
  - `git update-index --chmod=+x main/gradlew batch/gradlew`
  - `main/.gitignore`의 `!**/src/main/resources/key/` 예외 제거 (K-08)
  - 완료 기준: 트리가 [04 §7](04-target-architecture.md#7-목표-디렉토리-구조)와 일치(아직 없는 폴더 제외), `git grep -nE 'sk-[A-Za-z0-9]{20}|BEGIN PRIVATE KEY' -- ':!docs/restoration/'` 결과 없음, main 컴파일 성공

## Phase 1 — 인프라 골격

- [ ] **R-10** `compose.yml` / `compose.override.yml` / `.env.example`: mysql, redis, elasticsearch, seaweedfs, 볼륨·네트워크·헬스체크·메모리 제한(최소값, [04 §5](04-target-architecture.md#5-리소스-산정-메모리)), 환경변수([06 §6](06-db-and-config.md#6-환경변수-전체-목록)). 완료 기준: `docker compose config --quiet` 통과, 인프라 4종만 부분 기동해 healthy·OOM 없음, `down` 후 재기동해도 데이터 유지
- [ ] **R-11** Flyway: `flyway` one-shot 서비스, `V1__init_schema.sql`(master 엔티티 기준, [06 §2](06-db-and-config.md#2-스키마-관리-flyway-d-20)), `V2__spring_batch_schema.sql`, 개발용 소량 시드 `infra/db/seed/`. 완료 기준: flyway 성공 종료, main이 `ddl-auto: validate`로 기동 *(V1은 R-20 이후 Boot 3.5 Hibernate로 생성 — R-20과 함께 진행 가능)*
- [ ] **R-12** `infra/mysql/initdb/`: exporter 계정 생성 스크립트, 문자셋·시간대 설정
- [ ] **R-13** SeaweedFS: `s3.json` 템플릿 + entrypoint(환경변수 렌더링), anonymous Read, `storage-init`(aws-cli로 버킷·CORS). 완료 기준: aws-cli presigned PUT 업로드 성공, 공개 URL로 GET 성공
- [ ] **R-14** 모니터링 인프라: prometheus(`infra/monitoring/prometheus/prometheus.yml`), grafana provisioning, cadvisor, mysqld/redis/es exporter, profile `monitoring`. 완료 기준: 인프라 + 모니터링만 부분 기동해 인프라 타깃 UP, cAdvisor가 Docker Desktop에서 동작하는지·`--disable_metrics` 플래그 확인·기록

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
- [ ] **R-32** Lost112 수집 개선: 키 URL 인코딩, 페이지 단위 파싱 + bulk 인덱싱(512MB 제한 내 동작), 수집 기간 설정, `atcId` 문서 ID. 샘플 문서 적재 스크립트 `infra/elasticsearch/seed/` ([06 §8](06-db-and-config.md#8-시드더미-데이터)). 완료 기준: 명세서 응답 예시로 만든 XML 픽스처로 파싱·인덱싱 테스트 통과, 샘플 적재 후 `GET /search/total` > 0. 실제 API 수집은 U-05 후 검증(1차에서는 "미검증", D-37)
- [ ] **R-33** ES 매핑 명시([06 §3](06-db-and-config.md#3-elasticsearch-d-06)), 매칭 로그 결정적 ID
- [ ] **R-34** 잡·스케줄 복원: `policeJob`(수집 on/off + Lost112 매칭), `findearJob`, 수동 트리거 유지. 완료 기준: 짧은 cron으로 두 잡 실행 → 매칭 로그 적재
- [ ] **R-35** main↔batch 계약 검증 ([07 §2](07-api-contracts.md#2-main--batch)): 분실물 등록 → 매칭 → 매칭 목록 조회, Lost112 목록·스크랩 end-to-end
- [ ] **R-36** 정리: 주석 처리된 FCM·alarm 코드 삭제, 위험 엔드포인트 local 한정 ([07 §3](07-api-contracts.md#3-batch-api-전체-팀-버전와-1차-처리)), `new RestTemplate()` → 빈

## Phase 4 — match mock (Phase 2와 병렬 가능)

- [ ] **R-40** `match/` 신규 Spring Boot 3.5 앱: [07 §5](07-api-contracts.md#5-match-mock-동작-명세-r-40-d-28) 명세대로 3개 API, `MatchingScorer` 인터페이스, 관리 포트 8085, Dockerfile. 완료 기준: JSON 픽스처 계약 테스트 통과, batch·main과 연동 동작

## Phase 5 — 모니터링 연결

- [ ] **R-50** 앱 지표: 3개 앱 actuator/prometheus, `application` 태그, WebClient·RestTemplate을 Builder 빈으로(K-09), 커스텀 지표([04 §6](04-target-architecture.md#6-모니터링-설계-d-18))
- [ ] **R-51** Grafana 대시보드: 후보 ID 대시보드 JSON 커밋 + "Findear Overview" 작성. 완료 기준: 모니터링 + 대상 일부(예: main과 그 의존 서비스)만 부분 기동해 해당 타깃 UP, 관련 패널에 데이터 표시. 전체 타깃 동시 확인과 메모리 실측은 1차 범위 외 (D-32)

## Phase 6 — 배포 준비 (P5, [09](09-deploy-and-aws.md))

- [ ] **R-60** `.github/workflows/ci.yml`: PR·push 시 main/batch/match 빌드·테스트 (U-03)
- [ ] **R-61** `.github/workflows/images.yml`: master push 시 GHCR 이미지 빌드·푸시(`ghcr.io/ehighg/findear-{main,batch,match}`, 태그 `sha`·`latest`). 첫 푸시 후 패키지 visibility public 확인
- [ ] **R-62** `compose.prod.yml`: GHCR 이미지, main `80:8080`, Redis·ES 비밀번호/보안 on, 모니터링 127.0.0.1 바인딩, node-exporter, `restart`, 로그 로테이션
- [ ] **R-63** `infra/deploy/init-host.sh`(Ubuntu: Docker, `vm.max_map_count`, swap), `deploy.sh`(pull → up, `IMAGE_TAG` 롤백)
- [ ] **R-64** AWS S3 연동 키트 `infra/aws/` ([09 §4](09-deploy-and-aws.md#4-aws-s3-연동-키트))
- [ ] **R-65** (선택) `deploy.yml`: workflow_dispatch로 SSH 배포 (시크릿 이름만 문서화)

## Phase 7 — 검증 도구

- [ ] **R-80** `tools/fcm-test/`: `index.html` + `firebase-messaging-sw.js` + `firebase-config.example.js`, `python3 -m http.server 5500 -d tools/fcm-test`로 실행. 흐름: 테스트 로그인으로 JWT → 알림 권한 → `getToken(VAPID)` → `POST /notification/new` → `POST /alarm/send-fcm/{memberId}`

## Phase 8 — 1차 목표 최종 검증

- [ ] **R-90** 아래 시나리오를 처음부터 끝까지 수행하고 결과를 [10-worklog.md](10-worklog.md)에 기록. 외부 키가 아직 없으면 해당 단계만 건너뛰고 "미검증"으로 표시. **전체 동시 기동은 하지 않고(D-32), 묶음별로 필요한 서비스만 띄운 뒤 묶음이 끝나면 `docker compose down`.**
  1. 구성 검증: 깨끗한 clone → `cp .env.example .env`(값 채움) → `docker compose config --quiet`, `docker compose -f compose.yml -f compose.prod.yml config --quiet` 통과. 앱 이미지 3개 `docker compose build` 성공
  - **묶음 A — 저장소·자동채움** (`mysql flyway redis seaweedfs storage-init match main`): 기동 서비스 healthy, `flyway`·`storage-init` exit 0
    2. 테스트 로그인(local): `POST /members/login` `{"phoneNumber": "<시드 회원 번호>"}` → accessToken 획득
    3. 이미지: `POST /images/presign` → `curl -X PUT --upload-file a.jpg -H 'Content-Type: image/jpeg' "<uploadUrl>"` → 응답의 `url`로 GET 200
    4. 습득물 등록(MANAGER 회원): `POST /acquisitions`(이미지 key 포함) → 잠시 후 `GET /acquisitions/{boardId}`에 mock이 채운 category·color·description
  - **묶음 B — 매칭·검색** (모니터링 제외 핵심 서비스: 묶음 A + `elasticsearch batch`)
    5. 분실물 등록(NORMAL 회원): `POST /losts` → batch `/findear/matching` → match mock → `GET /matchings/findear/bests`에 결과. FCM 설정 시 테스트 페이지(R-80)에 알림
    6. Lost112: 샘플 문서 적재(`infra/elasticsearch/seed/`) → main `GET /acquisitions/lost112?…` 목록과 `GET /acquisitions/lost112/total-page`. 실제 API 수집(`POST /search/save`)은 U-05 후 (1차에서는 미검증, D-37)
    7. 배치 잡: `FINDEAR_JOB_CRON`·`POLICE_JOB_CRON`을 짧게 → 매칭 로그 증가, `GET /matchings/lost112/bests`
    8. 쪽지: `POST /message` → 상대에게 FCM 알림 (설정 시)
    9. Naver 로그인(U-06, U-01 후): authorize → 콜백 code → `GET /members/after-login?code=…`로 JWT. 1차에서는 미검증 예상 (D-37)
    10. VWorld(U-07): `GET /location/search?query=서울역&page=1&size=5`
  - **묶음 C — 모니터링** (`mysql flyway redis match main` + `prometheus grafana cadvisor mysqld-exporter redis-exporter`)
    11. `http://localhost:9090/targets`에서 띄운 서비스의 타깃 UP, Grafana "Findear Overview"의 main·JVM·MySQL·Redis·컨테이너 패널에 데이터. batch·ES 지표는 R-50·R-51에서 부분 기동으로 확인한 결과를 기록
  12. 비밀값 커밋 여부 최종 확인 (위 "비밀값 검사"). 자원 실측은 하지 않음 (D-32)
  - 완료 기준: 1~12 통과(또는 키 미발급으로 미검증 항목 명시) → [README](README.md#3-1차-목표-완료-기준-definition-of-done)의 DoD 충족

## 1차 목표 이후 (기록만)
- 프론트 재구축 (P3): 기능 유지·디자인 전면 수정, presigned 업로드·Naver(`client_secret` 제외)·FCM·SSE 계약 반영, O-5 결정
- HTTPS 재검토 (O-7), 알림(Alerting)·로그 수집(Loki), Java 21·Boot 4 전환
