# 08. 작업 계획 · 진행 트래커

> 사용법: 작업은 R-xx 단위. 시작하면 상태를 `진행중`, 끝나면 `[x]` + `완료(날짜)`로 바꾸고 [10-worklog.md](10-worklog.md)에 기록.
> 순서: Phase 0 → 1 → (2 ∥ 4) → 3 → 5 → 6 → 7 → 8(최종 검증). **세션은 Phase 단위**로 진행하고, Phase가 끝나면 멈춰서 사용자에게 보고한다 (D-35).
>
> **진행 절차 (D-33, D-34)**
> 1. Phase 착수 시 GitHub 이슈 1개 생성 (상위 이슈 **#12**의 sub-issue: `gh issue create --parent 12 …`). 템플릿은 `.github/ISSUE_TEMPLATE/simple-issue-template.md`, 작업 목록에 그 Phase의 R-xx를 체크박스로.
> 2. R-xx마다 master에서 브랜치 `{feature|fix|test}/{Phase 이슈번호}-{이름}` 생성. 커밋 `Type: 한국어 설명` (이 단계에선 이슈번호 없음).
> 3. 완료 기준 통과 → 비밀값 검사(아래) → 작업 브랜치 원격 push (원격 작업 브랜치는 세부 기록으로 남김).
> 4. 로컬에서 `ISSUE_REF='Related to #{Phase 이슈번호}' git rebase -x 'sh tools/git/add-issue-ref.sh' master`로 각 커밋 본문 끝(트레일러 앞)에 이슈 참조 추가 (D-39) → master에 fast-forward 병합 → `git push origin master` → 로컬 브랜치 삭제.
> 5. Phase의 R-xx가 모두 끝나면 이슈 작업 목록 체크 후 이슈 닫기, 상위 이슈 #12의 Phase 체크, 문서 갱신, 사용자에게 보고.
>
> **비밀값 검사 (push 전 매번, D-37)**: `git diff --cached`/`git diff origin/master...HEAD`를 눈으로 확인하고, `git grep -nIE 'sk-[A-Za-z0-9]{20}|BEGIN (RSA |EC )?PRIVATE KEY|AKIA[0-9A-Z]{16}|"private_key"|client_secret\s*[:=]\s*[A-Za-z0-9_-]{8}' -- ':!docs/restoration/'` 결과가 없어야 한다 (이 문서 자체가 패턴 문자열을 담고 있어 `docs/restoration/`은 제외). `.env`, `secrets/`, `firebase-config.js`가 추적되지 않는지 `git ls-files`로 확인. 예외: `front/.env`는 팀 시절부터 추적되는 파일로 URL만 있고 키 값은 비어 있다 (2026-09-30 확인, front는 1차 범위 밖이라 그대로 둠).
>
> **원본 레포 금지 (D-36)**: `2TF4/findear`에는 어떤 쓰기도 하지 않는다. gh 대상은 `.claude/settings.json`의 `GH_REPO=EhighG/Findear`로 고정돼 있다.
>
> **개발 중 전체 동시 기동 금지 (D-32)**: R-00~R-80에서는 compose 전체를 한 번에 띄우지 않고 자원도 실측하지 않는다. 전체 구성은 `docker compose config`로 검증, 동작 확인은 R-xx에 필요한 서비스만 부분 기동 후 `docker compose down`. **최종 검증(R-90)에서만 모니터링까지 전체를 띄우고 실측한다.**
>
> **외부 API 호출 금지 (D-38)**: Naver 로그인, VWorld, Firebase(FCM), 공공데이터포털 Lost112, AWS는 작업·검증 중 호출하지 않는다 (키 없이 보내는 요청 포함, 공식 문서 열람은 허용). 공식 문서 기준으로 구현하고 mock 계약 테스트로 검증해서, **사용자가 마지막에 키만 세팅하면 바로 동작**하게 만든다. 실제 동작 확인은 R-91(사용자). **AWS(S3·IAM·EC2)는 실제 연결이 필요한 검증 자체를 생략**하고, 로컬에서 같은 동작을 볼 수 있는 부분(SeaweedFS, `compose.prod.yml` config)과 문법 검사까지만 한다 (D-41).

## 사용자 작업 (코드 밖)

| ID | 작업 | 필요 시점 | 상태 |
|---|---|---|---|
| U-01 | Naver Developers에서 Client Secret **재발급** (`Chore/10-reset_env` 커밋 노출분) | 1차 작업 완료 후 (D-37) | [ ] |
| U-02 | GitHub 설정 → Code security → Secret scanning + Push protection 활성화 | 1차 작업 완료 후 (D-37). 그동안 Claude가 push 전 비밀값 검사 | [ ] |
| U-03 | (포크 레포) GitHub Actions 탭에서 워크플로 활성화 | R-60 | [ ] |
| U-04 | Firebase 새 프로젝트, 서비스계정 JSON, 웹앱 설정, VAPID 키 ([05 §2](05-external-integrations.md#2-firebase-cloud-messaging-웹푸시)) | 1차 작업 완료 후 → R-91 (D-38) | [ ] |
| U-05 | data.go.kr Lost112 API 2종 활용신청 + 트래픽 한도 확인 ([05 §3](05-external-integrations.md#3-공공데이터포털-lost112-api)) | 1차 작업 완료 후 → R-91 (D-37, D-38). R-32는 픽스처·샘플 데이터로 진행 | [ ] |
| U-06 | Naver 로그인 앱 등록 (callback `http://localhost:8080/members/login`, 테스트 ID). U-01과 같은 앱이면 함께 | 1차 작업 완료 후 → R-91 (D-38) | [ ] |
| U-07 | VWorld 인증키 발급 | 1차 작업 완료 후 → R-91 (D-38) | [ ] |
| U-08 | (유료, 배포 시에만) AWS 계정·EC2·S3 — `infra/aws/README.md` 절차. AWS·EC2 연결 확인도 이때 (1차 작업에서는 생략, D-41) | 배포 시 | [ ] |
| ~~U-09~~ | ~~이 문서 브랜치를 master에 병합~~ → Claude가 R-00에서 수행 (D-34) | – | – |
| U-10 | gh용 fine-grained PAT 갱신: 현재 토큰은 **2026-10-17 만료**. 새 토큰도 대상은 `EhighG/Findear`만, Repository permissions에 Issues: Read and write(이슈 생성·sub-issue 연결), 가능하면 Actions: Read(Phase 6 CI 결과 확인). git push는 Git Credential Manager 자격증명이라 별개 | 2026-10-17 전 | [ ] |

## Phase 0 — 정리

이슈 #13 (상위 #12). **완료 2026-09-30.**

- [x] **R-00** 작업 준비 (옛 이슈 #1~#11은 사용자가 이미 닫음, D-33) — 완료(2026-09-30)
  - 상위 이슈 #12 "Findear 복구 1차" 생성(본문: README 링크, Phase 목록), Phase 0 이슈 #13 생성 후 sub-issue로 연결
  - gh 안전장치 확인 (D-36): `echo $GH_REPO` = `EhighG/Findear`, `gh repo set-default --view` = `EhighG/Findear`
  - 문서 브랜치(`claude/happy-babbage-qt991n`)의 커밋 5개(작성 시점엔 3개)에 `Related to #12`를 붙여 master에 병합·push (구 U-09, `d4f6025..bf51b1b`). 원격·로컬 `claude/happy-babbage-qt991n`은 병합 후 삭제
  - 완료 기준: master에 `docs/restoration/`, `CLAUDE.md`, `.claude/settings.json` 존재, 상위 이슈·Phase 0 이슈 생성됨
  - 진행 중 gh의 fine-grained PAT에 Issues 쓰기 권한이 없어 이슈 생성이 막힘 → 사용자가 Issues: Read and write 추가 (U-10 참고)
- [x] **R-01** `Chore/10-reset_env` 원격 브랜치 삭제 (`git push origin --delete Chore/10-reset_env`). 내용은 [02 §6](02-current-state.md#6-브랜치)에 보존됨. 사용자 승인(2026-09-29). — 완료(2026-09-30, 삭제 전 `76edc42` 확인)
- [x] **R-02** 레거시 정리 (D-22) — 완료(2026-09-30, 브랜치 `feature/13-legacy-cleanup`)
  - 삭제: `config/`, stub `batch/`, `old-servers/match/`, `infra/findear-infra-setting/`, `infra/git-settings/`, `.gitlab/`
  - 이동: `old-servers/batch/` → `batch/`, `exec/포팅 매뉴얼.md`·`exec/서비스 시연 시나리오.pdf` → `docs/legacy/`, `exec/data/mainDB/*` → `infra/db/dummy/` (`batchDB_RDB-version/` 삭제)
  - 루트 `.gitignore`(`.env`, `!.env.example`, `secrets/`, `**/build/`, `.gradle/`, `.idea/`, `*.iml`, `.DS_Store`, `tools/fcm-test/firebase-config.js`), `.gitattributes`(`*.sh`·`gradlew` LF)
  - `git update-index --chmod=+x main/gradlew batch/gradlew`
  - `main/.gitignore`의 `!**/src/main/resources/key/` 예외 제거 (K-08)
  - 완료 기준: 트리가 [04 §7](04-target-architecture.md#7-목표-디렉토리-구조)와 일치(아직 없는 폴더 제외), `git grep -nE 'sk-[A-Za-z0-9]{20}|BEGIN PRIVATE KEY' -- ':!docs/restoration/'` 결과 없음, main 컴파일 성공
  - 결과: 위 정규식과 상단 "비밀값 검사" 정규식 모두 0건, main `compileJava`·`compileTestJava` 성공, `LostBoardQueryServiceTest` 4/4 (JDK 21, Windows Git Bash에서 `./gradlew`)
  - 계획 외로 함께 한 것: `infra/README.md`(git 훅 설치 안내) 삭제 / K-08은 예외 줄만으로는 효과가 없어서(원래 그 폴더를 제외하는 규칙이 없었음) `src/main/resources/key/`를 명시적으로 제외 / 루트 `.gitignore`에 `.env.*`·`*-firebase-adminsdk-*.json`, `.gitattributes`에 `*.bat` CRLF·`*.jar` binary (Gradle 기본값) / `batch/.gitignore`의 `*.yml`·`.json` 제외 규칙 제거 (팀 시절 Config Server용 규칙이라, 두면 Phase 3의 `application.yml`이 커밋되지 않음) / 이슈 참조 rebase 보조 스크립트 `tools/git/add-issue-ref.sh` (D-39)

## Phase 1 — 인프라 골격

이슈 #14 (상위 #12).

- [x] **R-10** `compose.yml` / `compose.override.yml` / `.env.example`: mysql, redis, elasticsearch, seaweedfs, 볼륨·네트워크·헬스체크·메모리 제한(최소값, [04 §5](04-target-architecture.md#5-리소스-산정-메모리)), 환경변수([06 §6](06-db-and-config.md#6-환경변수-전체-목록) 중 이 작업에서 쓰는 것, D-43). 완료 기준: `docker compose config --quiet` 통과, 인프라 4종만 부분 기동해 healthy·OOM 없음, `down` 후 재기동해도 MySQL·ES 데이터 유지 (SeaweedFS는 R-13에서 버킷·객체로 확인, Redis는 영속화 없음 D-25) — 완료(2026-09-30, `feature/14-compose-infra`)
  - 결과: config 통과, 4종 약 20초 만에 healthy, OOM·재시작 없음. `down` → `up` 후 MySQL 행·ES 문서 유지, Redis 키는 사라짐(의도). MySQL `utf8mb4`/`utf8mb4_0900_ai_ci`/`+09:00` 확인 (R-12의 문자셋·시간대는 compose의 `command`로 처리). MySQL 헬스체크는 TCP(`-h 127.0.0.1`)라 초기화용 임시 서버(`port: 0`)가 끝난 뒤에 healthy가 됨을 로그로 확인
  - 호스트 포트를 `*_HOST_PORT`로 바꿀 수 있게 함 (D-44, 개발 PC는 MySQL80이 3306 사용 → `.env`에 3307)
  - SeaweedFS 메모 (R-13·R-14용): `/healthz` 200, 메트릭(9327)은 컨테이너 네트워크 주소에서만 열림(Prometheus 수집엔 문제없음), 기본값으로 Iceberg(8181)·Lance(9101) 리스너도 뜸, 로그에 IAM/STS 설정 없음 오류 1건(`no signing key found for STS service`)과 SSE-S3 KEK 경고
- [x] **R-11a** Flyway (R-11 분할, D-40): `flyway` one-shot 서비스(`infra/db/migration/`), `V1__spring_batch_schema.sql`(Boot 3.5.x가 관리하는 spring-batch-core 5.2.x의 `schema-mysql.sql`, [06 §2](06-db-and-config.md#2-스키마-관리-flyway-d-20)). 완료 기준: 빈 DB에서 flyway exit 0·`BATCH_*` 테이블 생성, 다시 실행해도 exit 0 (추가 적용 없음). main 스키마·시드는 Phase 2의 R-11b — 완료(2026-09-30, `feature/14-flyway`)
  - 결과: Boot 3.5.16(Maven Central의 3.5.x 최신)이 관리하는 spring-batch-core **5.2.6**의 `schema-mysql.sql` 원문 + 출처 주석. 빈 DB에서 exit 0, `BATCH_*` 9개 테이블과 시퀀스 행 3개 생성, `flyway_schema_history`에 v1 success. 다시 실행하면 "up to date. No migration necessary"로 exit 0
  - Flyway OSS 13.8.1 이미지에는 MySQL Connector/J가 없고 MariaDB Connector/J 2.7.14만 있음 → `FLYWAY_URL`에 `allowPublicKeyRetrieval=true` ([06 §1](06-db-and-config.md#1-mysql))
- [ ] **R-12** `infra/mysql/initdb/`: exporter 계정 생성 스크립트, 문자셋·시간대 설정
- [ ] **R-13** SeaweedFS: `s3.json` 템플릿 + entrypoint(환경변수 렌더링), anonymous Read, `storage-init`(aws-cli로 버킷·CORS). 완료 기준: `storage-init` exit 0 (다시 실행해도 성공), aws-cli 서명 업로드 성공, 공개 URL로 익명 GET 200·익명 PUT 거부, CORS preflight에 허용 origin·메서드 응답, `aws s3 presign`(GET) URL을 호스트에서 열면 200, `down` 후 재기동해도 버킷·객체 유지. presigned PUT은 R-24에서 확인 (aws-cli `s3 presign`은 GET만 지원, D-42)
- [ ] **R-14** 모니터링 인프라: prometheus(`infra/monitoring/prometheus/prometheus.yml`), grafana provisioning, cadvisor, mysqld/redis/es exporter, profile `monitoring`. 완료 기준: 인프라 + 모니터링만 부분 기동해 인프라 타깃 UP, cAdvisor가 Docker Desktop에서 동작하는지 확인·기록

## Phase 2 — main 복구

- [ ] **R-20** 빌드 정비: Boot 3.2.3 → 3.5.x, mail·mariadb 의존성·`httpBasic` 제거, firebase-admin 9.x, Querydsl 5.1.0, `micrometer-registry-prometheus` 추가, 멀티스테이지 Dockerfile(런타임에 curl). 완료 기준: 빌드·단위테스트 통과, 이미지 빌드 성공
- [ ] **R-11b** main 스키마·시드 (R-11 분할, D-40. R-20 다음, R-21 전): `V2__init_schema.sql`(master 엔티티 기준, R-20의 Boot 3.5 Hibernate로 생성, [06 §2](06-db-and-config.md#2-스키마-관리-flyway-d-20)), 개발용 소량 시드 `infra/db/seed/`([06 §8](06-db-and-config.md#8-시드더미-데이터), 로컬 전용). 완료 기준: 빈 DB에서 flyway가 V1·V2 적용 후 exit 0, 시드 적용 성공. main의 `validate` 기동 확인은 R-21
- [ ] **R-21** 설정 외부화: `application.yml`/`-local`/`-prod`([06 §7](06-db-and-config.md#7-설정-파일-구조-각-spring-앱)), `profiles.active: secret` 폐기, VWorld 키·CORS origin·서버 URL 환경변수화, 외부 API 주소(VWorld, Naver)는 설정값으로(기본값은 공식 주소, 테스트에서 mock 서버로 교체, D-38), 관리 포트 8081. 완료 기준: `git grep -n j10a706 main/` 0건, 하드코딩 키 0건, 외부 키가 비어 있어도 flyway 스키마(R-11b)에 `ddl-auto: validate`로 기동
- [ ] **R-22** 버그 수정: K-01(Lost112 목록·총개수 경로 `/search`), Lombok `@Builder.Default` 경고 정리
- [ ] **R-23** FCM 복구: 공식 문서(Firebase Admin SDK Java, FCM HTTP v1 웹푸시) 기준으로 firebase-admin 9.x 초기화·발송 코드 정비, `fcm.enabled` 조건부 초기화, `FCM_CREDENTIALS_PATH`, `secrets/` 마운트 (K-03). 완료 기준: 키 없이 기동하고 발송 단계는 오류 없이 건너뜀, 발송 로직 단위 테스트(Firebase 호출 없이 메시지 구성·토큰 조회·실패 처리) 통과. 실제 발송은 R-91
- [ ] **R-24** 스토리지: AWS SDK v2 `S3Client`(내부 엔드포인트) + `S3Presigner`(공개 엔드포인트), `POST /images/presign`, 게시글 등록 시 object key 저장·URL 조립 (D-13). 완료 기준: curl로 presign → PUT → 게시글 등록 → 조회 응답에 이미지 URL (presigned PUT은 여기서 처음 확인, D-42)
- [ ] **R-25** Naver 로그인: 공식 문서(네이버 로그인 API 명세) 기준으로 인가 코드 → 토큰 교환 → 프로필 조회의 요청 파라미터·응답·오류 처리 점검·수정. (선택) 고정 `state` 개선. 완료 기준: 공식 문서의 응답 예시·오류 응답을 재현한 mock 서버 계약 테스트로 회원 조회/가입 → JWT 발급까지 통과, 키 미설정 시 "설정 필요" 오류 응답. 실제 로그인은 R-91
- [ ] **R-26** VWorld: 공식 문서(검색 API 2.0, 주소→좌표 변환 API 2.0) 기준으로 `/location/search`, `/location/address`의 요청 파라미터·응답·오류 처리 점검·수정. 완료 기준: mock 서버 계약 테스트 통과, 키 미설정 시 "설정 필요" 오류 응답. 실제 호출은 R-91
- [ ] **R-27** 보안 정리 (D-26): K-06(전화번호 로그인·테스트 가입), K-07(`/alarm/send-*`)을 local 프로필 한정, SSE 구독은 본인 ID만. (선택) Redis key serializer 개선, Testcontainers로 `MainApplicationTests` 복구

## Phase 3 — batch 복구

- [ ] **R-30** Boot 3.5 마이그레이션 ([02 §3](02-current-state.md#3-팀-batch-old-serversbatch--복원-대상)): jakarta, Spring Batch 5 API, `@EnableBatchProcessing` 제거, `RestHighLevelClient` → Spring Data ES 5.5(`ElasticsearchOperations`), Spring Cloud 제거, 멀티스테이지 Dockerfile. 완료 기준: compose의 MySQL·ES에 붙어 기동, `validate` 통과
- [ ] **R-31** 설정 외부화: match URL, Lost112 키·URL(https), cron·on/off, 관리 포트 8083 ([06 §6](06-db-and-config.md#batch))
- [ ] **R-32** Lost112 수집 개선 (공식 명세 기준: 공공데이터포털 활용가이드, D-38): 키 URL 인코딩, 페이지 단위 파싱 + bulk 인덱싱(512MB 제한 내 동작), 수집 기간 설정, `atcId` 문서 ID. 샘플 문서 적재 스크립트 `infra/elasticsearch/seed/` ([06 §8](06-db-and-config.md#8-시드더미-데이터)). 완료 기준: 명세서 응답 예시로 만든 XML 픽스처로 파싱·인덱싱 테스트 통과, mock 서버로 페이지 순회·오류 응답(키 오류, 트래픽 초과) 처리 테스트 통과, 샘플 적재 후 `GET /search/total` > 0. 실제 API 수집은 R-91
- [ ] **R-33** ES 매핑 명시([06 §3](06-db-and-config.md#3-elasticsearch-d-06)), 매칭 로그 결정적 ID
- [ ] **R-34** 잡·스케줄 복원: `policeJob`(수집 on/off + Lost112 매칭), `findearJob`, 수동 트리거 유지. 완료 기준: 짧은 cron으로 두 잡 실행 → 매칭 로그 적재
- [ ] **R-35** main↔batch 계약 검증 ([07 §2](07-api-contracts.md#2-main--batch)): 분실물 등록 → 매칭 → 매칭 목록 조회, Lost112 목록·스크랩 end-to-end
- [ ] **R-36** 정리: 주석 처리된 FCM·alarm 코드 삭제, 위험 엔드포인트 local 한정 ([07 §3](07-api-contracts.md#3-batch-api-전체-팀-버전와-1차-처리)), `new RestTemplate()` → 빈

## Phase 4 — match mock (Phase 2와 병렬 가능)

- [ ] **R-40** `match/` 신규 Spring Boot 3.5 앱: [07 §5](07-api-contracts.md#5-match-mock-동작-명세-r-40-d-28) 명세대로 3개 API, `MatchingScorer` 인터페이스, 관리 포트 8085, Dockerfile. 완료 기준: JSON 픽스처 계약 테스트 통과, batch·main과 연동 동작

## Phase 5 — 모니터링 연결

- [ ] **R-50** 앱 지표: 3개 앱 actuator/prometheus, `application` 태그, WebClient·RestTemplate을 Builder 빈으로(K-09), 커스텀 지표([04 §6](04-target-architecture.md#6-모니터링-설계-d-18))
- [ ] **R-51** Grafana 대시보드: 후보 ID 대시보드 JSON 커밋 + "Findear Overview" 작성. 완료 기준: 모니터링 + 대상 일부(예: main과 그 의존 서비스)만 부분 기동해 해당 타깃 UP, 관련 패널에 데이터 표시. 전체 타깃 동시 확인과 메모리 실측은 R-90에서 (D-32)

## Phase 6 — 배포 준비 (P5, [09](09-deploy-and-aws.md))

- [ ] **R-60** `.github/workflows/ci.yml`: PR·push 시 main/batch/match 빌드·테스트 (U-03)
- [ ] **R-61** `.github/workflows/images.yml`: master push 시 GHCR 이미지 빌드·푸시(`ghcr.io/ehighg/findear-{main,batch,match}`, 태그 `sha`·`latest`). 첫 푸시 후 패키지 visibility public 확인
- [ ] **R-62** `compose.prod.yml`: GHCR 이미지, main `80:8080`, Redis·ES 비밀번호/보안 on, 모니터링 127.0.0.1 바인딩, node-exporter, `restart`, 로그 로테이션. 완료 기준: `docker compose -f compose.yml -f compose.prod.yml config --quiet` 통과. 배포 서버에서의 실행 확인은 생략 (D-41)
- [ ] **R-63** `infra/deploy/init-host.sh`(Ubuntu: Docker, `vm.max_map_count`, swap), `deploy.sh`(pull → up, `IMAGE_TAG` 롤백). 완료 기준: `bash -n` 통과. EC2에서의 실행 확인은 생략 (D-41)
- [ ] **R-64** AWS S3 연동 키트 `infra/aws/` ([09 §4](09-deploy-and-aws.md#4-aws-s3-연동-키트)). AWS 공식 문서(CLI `s3api`, IAM) 기준으로 작성하고 AWS는 호출하지 않음 (D-38). 완료 기준: 스크립트 `bash -n` 통과, 정책 JSON 문법 검사 통과, 같은 버킷·CORS 명령이 로컬 SeaweedFS(`storage-init`)에서 동작. AWS 전용 부분(Public Access Block, 버킷 정책, IAM)은 문법 검사까지 (D-41)
- [ ] **R-65** (선택) `deploy.yml`: workflow_dispatch로 SSH 배포 (시크릿 이름만 문서화). 실행 확인은 생략 (D-41)

## Phase 7 — 검증 도구

- [ ] **R-80** `tools/fcm-test/`: 공식 문서(Firebase JS SDK 웹 메시징) 기준 `index.html` + `firebase-messaging-sw.js` + `firebase-config.example.js`, `python3 -m http.server 5500 -d tools/fcm-test`로 실행 (로컬 Windows PC에서는 `python3`가 스토어 별칭이라 `python`). 흐름: 테스트 로그인으로 JWT → 알림 권한 → `getToken(VAPID)` → `POST /notification/new` → `POST /alarm/send-fcm/{memberId}`. 완료 기준: `firebase-config.js`가 없으면 Firebase를 초기화하지 않고 설정 안내만 표시하는 것까지 확인. 토큰 발급·알림 수신은 R-91
- [ ] **R-81** 외부 연동 키 세팅 가이드·확인 스크립트 (D-38): [05](05-external-integrations.md)에 "키 세팅 체크리스트"(연동별로 채울 `.env` 변수·`secrets/` 파일·콘솔 설정값), `tools/verify-external/`(README + `verify.sh`: `.env`·`secrets/` 누락 검사 → 설정된 연동만 main·batch 엔드포인트를 거쳐 확인 요청 → 결과 요약). 완료 기준: 키가 없는 지금 상태에서 실행하면 외부 호출 없이 "미설정" 항목만 보고하고 끝남, `bash -n` 통과. 외부 호출 경로는 R-91에서 사용자가 실행

## Phase 8 — 1차 목표 최종 검증

- [ ] **R-90** 아래 시나리오를 처음부터 끝까지 수행하고 결과를 [10-worklog.md](10-worklog.md)에 기록. **이 단계에서는 모니터링까지 전체를 한 번에 띄운다 (D-32).** 외부 키는 비워 둔 상태로 진행하고 외부 API는 호출하지 않는다 (D-38) — 외부 연동의 실제 동작은 R-91.
  1. 깨끗한 clone → `cp .env.example .env`(외부 키 제외한 값 채움) → `docker compose config --quiet`, `docker compose -f compose.yml -f compose.prod.yml config --quiet` 통과, `.env.example`이 [06 §6](06-db-and-config.md#6-환경변수-전체-목록)과 일치 (D-43) → `docker compose up -d --build`(`COMPOSE_PROFILES=monitoring`) → `docker compose ps`: 상시 서비스 전부 healthy, `flyway`·`storage-init`은 exit 0
  2. 테스트 로그인(local): `POST /members/login` `{"phoneNumber": "<시드 회원 번호>"}` → accessToken 획득
  3. 이미지: `POST /images/presign` → `curl -X PUT --upload-file a.jpg -H 'Content-Type: image/jpeg' "<uploadUrl>"` → 응답의 `url`로 GET 200
  4. 습득물 등록(MANAGER 회원): `POST /acquisitions`(이미지 key 포함) → 잠시 후 `GET /acquisitions/{boardId}`에 mock이 채운 category·color·description
  5. 분실물 등록(NORMAL 회원): `POST /losts` → batch `/findear/matching` → match mock → `GET /matchings/findear/bests`에 결과. FCM 비활성 상태에서 알림 단계가 오류 없이 건너뛰어짐
  6. Lost112: 샘플 문서 적재(`infra/elasticsearch/seed/`) → main `GET /acquisitions/lost112?…` 목록과 `GET /acquisitions/lost112/total-page`
  7. 배치 잡: `FINDEAR_JOB_CRON`·`POLICE_JOB_CRON`을 짧게(Lost112 수집은 off) → 매칭 로그 증가, `GET /matchings/lost112/bests`
  8. 쪽지: `POST /message`, `POST /message/reply` → 목록 조회 (FCM 비활성 상태에서 오류 없음)
  9. 외부 연동 미설정 상태 점검: `tools/verify-external/verify.sh` → 외부 호출 없이 미설정 항목 보고. VWorld·Naver 엔드포인트는 "설정 필요" 오류 응답
  10. 모니터링: `http://localhost:9090/targets` 전부 UP, Grafana "Findear Overview"와 가져온 대시보드(JVM, MySQL, Redis, ES, cAdvisor) 패널에 데이터
  11. 자원 실측: 2~10을 수행한 뒤 `docker stats --no-stream`으로 컨테이너별 메모리·CPU 기록, OOM 여부(`docker inspect -f '{{.State.OOMKilled}}'`) 확인 → [04 §5](04-target-architecture.md#5-리소스-산정-메모리) 표 갱신(부족한 서비스는 "여유" 값으로). 비밀값 커밋 여부 최종 확인 (위 "비밀값 검사")
  - 완료 기준: 1~11 통과 → [README](README.md#3-1차-목표-완료-기준-definition-of-done)의 DoD 충족. 여기까지가 Claude의 1차 작업

## 1차 작업 완료 후 — 사용자 (키 세팅)

- [ ] **R-91** 키 세팅 후 외부 연동 확인 (사용자, U-01·U-04~U-07 후): [05](05-external-integrations.md)의 "키 세팅 체크리스트"대로 `.env`·`secrets/`·`tools/fcm-test/firebase-config.js`를 채우고 `docker compose up -d` → `tools/verify-external/verify.sh`
  1. Naver 로그인: 브라우저 authorize → 콜백 code → `GET /members/after-login?code=…`로 JWT
  2. VWorld: `GET /location/search?query=서울역&page=1&size=5`, `GET /location/address?…`
  3. Lost112: `LOST112_COLLECT_ENABLED=true` → batch `POST /search/save`(또는 짧은 cron) → main `GET /acquisitions/lost112` 목록
  4. FCM: `FCM_ENABLED=true` → R-80 테스트 페이지에서 토큰 등록 → `POST /alarm/send-fcm/{memberId}` 알림 수신 → 분실물 등록 매칭 알림, 쪽지 알림
  - 실패하면 결과(응답·로그)를 공유 → Claude가 수정

## 1차 목표 이후 (기록만)
- 프론트 재구축 (P3): 기능 유지·디자인 전면 수정, presigned 업로드·Naver(`client_secret` 제외)·FCM·SSE 계약 반영, O-5 결정
- HTTPS 재검토 (O-7), 알림(Alerting)·로그 수집(Loki), Java 21·Boot 4 전환
