# 08. 작업 계획 · 진행 트래커

> 사용법: 작업은 R-xx 단위. 시작하면 상태를 `진행중`, 끝나면 `[x]` + `완료(날짜)`로 바꾸고 [10-worklog.md](10-worklog.md)에 기록.
> 순서: Phase 0 → 1 → (2 ∥ 4) → 3 → 5 → 6 → 7 → 8(최종 검증). **세션은 Phase 단위**로 진행하고, Phase가 끝나면 멈춰서 사용자에게 보고한다 (D-35).
>
> **진행 절차 (D-33, D-34, D-46)** — 실행·검증은 subagent가 하고 메인 세션은 지시서 작성과 아래 git·이슈·문서 단계를 맡는다 (흐름은 CLAUDE.md "작업 방식")
> 1. Phase 착수 시 GitHub 이슈 1개 생성 (상위 이슈 **#12**의 sub-issue: `gh issue create --parent 12 …`). 템플릿은 `.github/ISSUE_TEMPLATE/simple-issue-template.md`, 작업 목록에 그 Phase의 R-xx를 체크박스로.
> 2. R-xx마다 master에서 브랜치 `{feature|fix|test}/{Phase 이슈번호}-{이름}` 생성. 커밋 `Type: 한국어 설명` (이 단계에선 이슈번호 없음).
> 3. 완료 기준 통과(`findear-verifier` PASS) → 메인이 커밋 → 비밀값 검사(아래) → 작업 브랜치 원격 push (원격 작업 브랜치는 세부 기록으로 남김).
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
| U-03 | (포크 레포) GitHub Actions 탭에서 워크플로 활성화 | R-60 | [x] 이미 켜져 있음(2026-10-01 사용자 확인) |
| U-04 | Firebase 새 프로젝트, 서비스계정 JSON, 웹앱 설정, VAPID 키 ([05 §2](05-external-integrations.md#2-firebase-cloud-messaging-웹푸시)) | 1차 작업 완료 후 → R-91 (D-38) | [ ] |
| U-05 | data.go.kr Lost112 API 2종 활용신청 + 트래픽 한도 확인 ([05 §3](05-external-integrations.md#3-공공데이터포털-lost112-api)) | 1차 작업 완료 후 → R-91 (D-37, D-38). R-32는 픽스처·샘플 데이터로 진행 | [ ] |
| U-06 | Naver 로그인 앱 등록 (callback `http://localhost:8080/members/login`, 테스트 ID). U-01과 같은 앱이면 함께 | **추후** — Naver 로그인 복구(R-25)와 함께 (D-50) | [ ] |
| U-07 | VWorld 인증키 발급 | 1차 작업 완료 후 → R-91 (D-38) | [ ] |
| U-08 | (유료, 배포 시에만) AWS 계정·EC2·S3 — `infra/aws/README.md` 절차. AWS·EC2 연결 확인도 이때 (1차 작업에서는 생략, D-41) | 배포 시 | [ ] |
| ~~U-09~~ | ~~이 문서 브랜치를 master에 병합~~ → Claude가 R-00에서 수행 (D-34) | – | – |
| U-10 | gh용 fine-grained PAT 갱신: 현재 토큰은 **2026-10-17 만료**. 새 토큰도 대상은 `EhighG/Findear`만, Repository permissions에 Issues: Read and write(이슈 생성·sub-issue 연결), Actions: Read 이상(CI 결과 확인). **현재 토큰(사용자가 붙인 이름 AI-based-dev, OS 키링에 저장, `github_pat_…`)이 이미 두 권한을 가짐** — `gh api repos/EhighG/Findear/actions/runs` 200(2026-10-01 확인), 갱신 때 같은 권한으로. Actions **설정** 조회(`actions/permissions`)는 Administration: read가 필요해 403이지만 작업에 필요 없음. git push는 Git Credential Manager 자격증명이라 별개 | 2026-10-17 전 | [ ] |

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

이슈 #14 (상위 #12). **완료 2026-09-30.** 로컬 `.env` 만들기: `cp .env.example .env` (이 PC는 `MYSQL_HOST_PORT=3307`, D-44).

- [x] **R-10** `compose.yml` / `compose.override.yml` / `.env.example`: mysql, redis, elasticsearch, seaweedfs, 볼륨·네트워크·헬스체크·메모리 제한(최소값, [04 §5](04-target-architecture.md#5-리소스-산정-메모리)), 환경변수([06 §6](06-db-and-config.md#6-환경변수-전체-목록) 중 이 작업에서 쓰는 것, D-43). 완료 기준: `docker compose config --quiet` 통과, 인프라 4종만 부분 기동해 healthy·OOM 없음, `down` 후 재기동해도 MySQL·ES 데이터 유지 (SeaweedFS는 R-13에서 버킷·객체로 확인, Redis는 영속화 없음 D-25) — 완료(2026-09-30, `feature/14-compose-infra`)
  - 결과: config 통과, 4종 약 20초 만에 healthy, OOM·재시작 없음. `down` → `up` 후 MySQL 행·ES 문서 유지, Redis 키는 사라짐(의도). MySQL `utf8mb4`/`utf8mb4_0900_ai_ci`/`+09:00` 확인 (R-12의 문자셋·시간대는 compose의 `command`로 처리). MySQL 헬스체크는 TCP(`-h 127.0.0.1`)라 초기화용 임시 서버(`port: 0`)가 끝난 뒤에 healthy가 됨을 로그로 확인
  - 호스트 포트를 `*_HOST_PORT`로 바꿀 수 있게 함 (D-44, 개발 PC는 MySQL80이 3306 사용 → `.env`에 3307)
  - SeaweedFS 메모 (R-13·R-14용): `/healthz` 200, 메트릭(9327)은 컨테이너 네트워크 주소에서만 열림(Prometheus 수집엔 문제없음), 기본값으로 Iceberg(8181)·Lance(9101) 리스너도 뜸, 로그에 IAM/STS 설정 없음 오류 1건(`no signing key found for STS service`)과 SSE-S3 KEK 경고
- [x] **R-11a** Flyway (R-11 분할, D-40): `flyway` one-shot 서비스(`infra/db/migration/`), `V1__spring_batch_schema.sql`(Boot 3.5.x가 관리하는 spring-batch-core 5.2.x의 `schema-mysql.sql`, [06 §2](06-db-and-config.md#2-스키마-관리-flyway-d-20)). 완료 기준: 빈 DB에서 flyway exit 0·`BATCH_*` 테이블 생성, 다시 실행해도 exit 0 (추가 적용 없음). main 스키마·시드는 Phase 2의 R-11b — 완료(2026-09-30, `feature/14-flyway`)
  - 결과: Boot 3.5.16(Maven Central의 3.5.x 최신)이 관리하는 spring-batch-core **5.2.6**의 `schema-mysql.sql` 원문 + 출처 주석. 빈 DB에서 exit 0, `BATCH_*` 9개 테이블과 시퀀스 행 3개 생성, `flyway_schema_history`에 v1 success. 다시 실행하면 "up to date. No migration necessary"로 exit 0
  - Flyway OSS 13.8.1 이미지에는 MySQL Connector/J가 없고 MariaDB Connector/J 2.7.14만 있음 → `FLYWAY_URL`에 `allowPublicKeyRetrieval=true` ([06 §1](06-db-and-config.md#1-mysql))
- [x] **R-12** `infra/mysql/initdb/`: exporter 계정 생성 스크립트, 문자셋·시간대 설정 — 완료(2026-09-30, `feature/14-mysql-initdb`)
  - 결과: `01-exporter-user.sh`(실행 파일 100755)가 최초 초기화 때 `exporter`@`%` 생성: SELECT·PROCESS·REPLICATION CLIENT, 동시 접속 3. TCP 로그인·performance_schema 읽기 가능, 쓰기(CREATE)는 거부. 비밀번호는 `MYSQL_EXPORTER_PASSWORD`(compose에서 필수). 문자셋·시간대는 R-10의 compose `command`로 설정
  - Docker Desktop의 bind mount는 파일이 실행 가능으로 보여 entrypoint가 source가 아니라 실행함 → entrypoint 내부 함수(`docker_process_sql`) 대신 mysql 클라이언트를 직접 쓰고, git에서도 실행 파일로 고정해 호스트와 관계없이 같은 방식으로 돌게 함
- [x] **R-13** SeaweedFS: `s3.json` 템플릿 + entrypoint(환경변수 렌더링), ~~anonymous Read~~ → `images/*` 공개 읽기 버킷 정책(D-45), `storage-init`(aws-cli로 버킷·CORS·정책). 완료 기준: `storage-init` exit 0 (다시 실행해도 성공), aws-cli 서명 업로드 성공, 공개 URL로 익명 GET 200·익명 PUT 거부, CORS preflight에 허용 origin·메서드 응답, `aws s3 presign`(GET) URL을 호스트에서 열면 200, `down` 후 재기동해도 버킷·객체 유지. presigned PUT은 R-24에서 확인 (aws-cli `s3 presign`은 GET만 지원, D-42) — 완료(2026-09-30, `feature/14-seaweedfs`)
  - 결과 (검증 11항목 모두 통과, 외부 호출 없음): `storage-init` exit 0(다시 실행 시 "버킷 있음" 후 같은 설정 재적용), 서명 업로드·목록 성공, 잘못된 secret은 `SignatureDoesNotMatch`(aws-cli 종료 코드 254)로 거부, 익명 GET `images/*` 200·`private/*` 403·익명 PUT 403·익명 목록 403, CORS preflight는 허용 origin을 돌려주고 메서드 `PUT, GET, HEAD`, 다른 origin은 403, presigned GET(path-style, `http://localhost:8333`으로 서명) 200·서명을 바꾸면 403, `down` → `up` 후 업로드 없이도 객체·정책·CORS 유지, 128MB에서 OOM 없음
  - 파일: `infra/seaweedfs/s3.json.template`(앱 identity 하나), `entrypoint.sh`(자격증명 채운 뒤 `/tmp/s3.json`, seaweed 사용자만 읽기), `storage-init.sh`(`STORAGE_ENDPOINT`가 비면 AWS 기본 엔드포인트, R-64에서 재사용). 자격증명은 compose에서 필수로 두지 않고 스크립트에서 검사 (배포에서 IAM Role을 쓰면 비어 있는 게 정상)
  - SeaweedFS 로그의 `no signing key found for STS service` 오류는 자격증명 파일을 넣어도 남음. 쓰지 않는 STS 기능 로그이며 인증은 정상 동작(잘못된 키 거부 확인)
  - `.env.example`의 `ES_JAVA_OPTS`를 따옴표로 감쌈 (셸에서 `source`할 때 공백 때문에 깨지던 것, compose는 같은 값으로 읽음)
- [x] **R-14** 모니터링 인프라: prometheus(`infra/monitoring/prometheus/prometheus.yml`), grafana provisioning, cadvisor, mysqld/redis/es exporter, profile `monitoring`. 완료 기준: 인프라 + 모니터링만 부분 기동해 인프라 타깃 UP, cAdvisor가 Docker Desktop에서 동작하는지 확인·기록 — 완료(2026-09-30, `feature/14-monitoring-infra`)
  - 결과: 인프라 4 + 모니터링 6만 부분 기동, 15초 안에 헬스체크가 있는 9개 healthy, OOM·재시작 없음. Prometheus 타깃 6개(prometheus, mysql, redis, elasticsearch, cadvisor, seaweedfs) 모두 UP, `mysql_up`·`redis_up` 1, ES green. Grafana는 `.env` 관리자 계정으로만 로그인(기본 admin/admin 401), provisioning된 Prometheus 데이터소스(읽기 전용, 기본값)로 질의 성공. `COMPOSE_PROFILES`를 비우면 모니터링 서비스가 빠짐
  - 앱 수집 job은 R-50, 대시보드 provisioning은 R-51에서 추가 (지금 Grafana 로그의 "dashboard/plugin provisioning 폴더를 못 읽음" 경고·오류는 그 폴더가 아직 없어서 나는 것)
  - mysqld-exporter는 `--mysqld.address`·`--mysqld.username` + `MYSQLD_EXPORTER_PASSWORD` 환경변수로 동작함(`mysql_up`=1로 확인). redis-exporter는 scratch 이미지라 헬스체크를 두지 않음 → Prometheus `up{job="redis"}`로 판단
  - **cAdvisor on Docker Desktop(WSL2, cgroup v2)**: 동작함. 컨테이너 이름(`findear-*`)으로 구분되고 CPU·메모리(working set·usage·limit)·네트워크·블록 I/O 수집됨. 컨테이너별 파일시스템 사용량(`container_fs_usage_bytes`)은 수집되지 않음. `/etc/machine-id`가 없다는 경고만 있음 ([04 §6](04-target-architecture.md#접근보안)에도 기록)
  - 참고 (R-90용, 실측 아님): cAdvisor 동작 확인 중 ES의 working set이 약 0.9GiB/1GiB(페이지 캐시 포함)로 보였음. 메모리 값 조정은 R-90 실측에서 판단 (D-31, D-32)

## Phase 2 — main 복구

이슈 #15 (상위 #12). **완료 2026-09-30** — R-20·R-11b·R-21·R-22·R-23·R-24·R-26·R-27 완료, R-25(Naver)는 1차에서 제외(D-50). 작업 지시서 초안은 로컬 PC `.claude/work-orders/`(git 제외, `.git/info/exclude`)에 있음 — 없는 환경이면 아래 R-xx 설명으로 다시 작성.

- [x] **R-20** 빌드 정비: Boot 3.2.3 → 3.5.x, mail·mariadb 의존성·`httpBasic` 제거, firebase-admin 9.x, Querydsl 5.1.0, `micrometer-registry-prometheus` 추가, 멀티스테이지 Dockerfile(런타임에 curl). 완료 기준: 빌드·단위테스트 통과, 이미지 빌드 성공 — 완료(2026-09-30, `feature/15-boot35-build`)
  - 결과: Boot **3.5.16**(Framework 6.2.19, Security 6.5.11, Hibernate 6.6.53), dependency-management 1.1.7, Gradle wrapper **8.14.5**(스크립트·jar 포함), firebase-admin 9.11.0, Querydsl 5.1.0(`com.querydsl` jakarta), blaze-persistence 1.6.20, p6spy starter 1.12.1(Boot 3.x용 마지막 줄, 2.0.x는 Boot 4용). mail·mariadb·querydsl-sql 제거, `httpBasic` 제거, `micrometer-registry-prometheus` 추가. 업그레이드로 깨진 코드 없음. `compileJava`·`compileTestJava` 성공, `LostBoardQueryServiceTest` 4/4
  - `main/Dockerfile`: `gradle:8.14.5-jdk17`에서 `bootJar` → `eclipse-temurin:17.0.20.1_1-jre-noble`(curl 8.5.0 기본 포함), uid 10001, `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=50`, `/app/app.jar` 하나. 이미지 225MB. `.gitattributes`에 `Dockerfile`·`.dockerignore` LF 고정 추가
  - 경고: `@Builder will ignore the initializing expression` 14건(R-22), Security 6.5에서 제거 예정인 `AntPathRequestMatcher`(JwtFilter) 13건 → R-22에 추가. 전체 `test`·`build`는 DB가 필요한 `MainApplicationTests` 때문에 실패하는 기존 상태 그대로 (R-27)
  - 진행 중 한 번 FAIL: 처음 `wrapper` 작업을 8.5가 실행해 `gradle-wrapper.properties`만 바뀜 → 8.14.5로 한 번 더 실행해 해결. **wrapper를 올릴 땐 `wrapper` 작업을 두 번 실행**해야 스크립트·jar까지 새 버전이 된다
- [x] **R-11b** main 스키마·시드 (R-11 분할, D-40. R-20 다음, R-21 전): `V2__init_schema.sql`(master 엔티티 기준, R-20의 Boot 3.5 Hibernate로 생성, [06 §2](06-db-and-config.md#2-스키마-관리-flyway-d-20)), 개발용 소량 시드 `infra/db/seed/`([06 §8](06-db-and-config.md#8-시드더미-데이터), 로컬 전용). 완료 기준: 빈 DB에서 flyway가 V1·V2 적용 후 exit 0, 시드 적용 성공. main의 `validate` 기동 확인은 R-21 — 완료(2026-09-30, `feature/15-main-schema`)
  - 결과: V2는 Hibernate 6.6.53 + MySQL 8.4 dialect로 생성한 DDL에서 순서·제약 이름만 정리 (13개 테이블, 인덱스 2, UNIQUE 3, FK 14). 다시 생성한 DDL과 정규화 비교해 타입·NULL·길이·기본값 차이 0. 임시 테스트로 flyway가 만든 DB에 Hibernate `validate` 통과(컬럼 하나를 바꾸면 실패하는 것도 확인). 빈 DB에서 V1·V2·시드 적용 exit 0, 재실행 "up to date", `-f compose.yml`만 쓰면 시드 없이 V1·V2만
  - 결정: 이미지 컬럼은 `img_url` 그대로, 정리는 R-24의 V3 (D-47). 시드는 Flyway 반복 마이그레이션을 override에서만 적용 (D-48)
  - 시드를 mysql 클라이언트로 다시 실행하면 컨테이너 클라이언트의 기본 문자셋(latin1) 때문에 한글이 깨지는 것을 검증에서 발견 → 시드에 `SET NAMES utf8mb4` 추가, 클라이언트로 두 번 다시 실행해도 UTF-8 유지 확인
  - 참고: 엔티티 `Lost112Scrap.lost112AtcId`에 `@Column`과 `@JoinColumn`이 같이 붙어 있음 (스키마엔 영향 없음, 정리 후보)
- [x] **R-21** 설정 외부화: `application.yml`/`-local`/`-prod`([06 §7](06-db-and-config.md#7-설정-파일-구조-각-spring-앱)), `profiles.active: secret` 폐기, VWorld 키·CORS origin·서버 URL 환경변수화, 외부 API 주소(VWorld, Naver)는 설정값으로(기본값은 공식 주소, 테스트에서 mock 서버로 교체, D-38), 관리 포트 8081. 완료 기준: `git grep -n j10a706 main/` 0건, 하드코딩 키 0건, 외부 키가 비어 있어도 flyway 스키마(R-11b)에 `ddl-auto: validate`로 기동 — 완료(2026-09-30, `feature/15-main-config`)
  - 결과: `application.yml`(모든 값 `${ENV:기본값}`, `MYSQL_PASSWORD`·`JWT_SECRET`은 기본값 없음, 외부 키는 빈 기본값) / `-local`(루트 `.env` import, 모드 B에서 `DB_PORT`·`REDIS_PORT`가 `*_HOST_PORT`를 따라감, SQL·p6spy 로그) / `-prod`(p6spy 로그 끔). VWorld 키·`j10a706` origin·서버 주소 하드코딩 제거, 관리 포트 8081, `RedisConfig`는 Boot 자동 설정 팩토리 사용(`REDIS_PASSWORD` 반영). compose `main` 서비스(이미지 `${IMAGE_REGISTRY:-ghcr.io/ehighg}/findear-main:${IMAGE_TAG:-latest}`, 로컬은 override의 `build`), `.env.example`에 main 변수
  - 확인: 외부 키 없이 `mysql flyway redis main` 부분 기동 → healthy, `validate` 통과, 시드 회원 로그인으로 accessToken, 관리 포트는 컨테이너 안에서만 UP·`application="main"` 지표, prod 프로필 기동, 모드 B(`bootRun`)도 `.env`만으로 기동·로그인. 기동 시 `JwtAuthenticationProvider`의 blaze window function 쿼리(`row_number() over`)가 Hibernate 6.6에서 정상
  - 발견·수정: Accept가 없거나 `*/*`이면 응답이 **XML**이던 것 → firebase-admin 9.11.0 → google-cloud-storage가 `jackson-dataformat-xml`을 끌어오고 `@EnableWebMvc`라 XML 변환기가 먼저 선택됨. `WebConfig`에서 기본 콘텐츠 타입을 JSON으로 (R-20에서 생긴 회귀)
  - 발견(다른 R-xx로): `GET /losts`가 `sortBy` 없으면 NPE(K-13 → R-22), 401 응답 본문이 비어 있음·`Using generated security password` 경고(R-27 후보), 이 PC는 호스트 8080을 다른 프로젝트가 써서 `.env`에 `MAIN_HOST_PORT=8090`
- [x] **R-22** 버그 수정: K-01(Lost112 목록·총개수 경로 `/search`), K-13(`GET /losts` `sortBy` 없을 때 NPE), Lombok `@Builder.Default` 경고 정리, Spring Security 6.5에서 제거 예정인 `AntPathRequestMatcher`(JwtFilter) 교체 (R-20에서 발견) — 완료(2026-09-30, `fix/15-main-bugs`)
  - 결과: K-01 — Lost112 목록 `{batch}/search?page&size[&category][&startDate&endDate][&keyword]`, 총개수 `/search/total` (`UriComponentsBuilder`, 한글 한 번만 인코딩). K-13 — `sortBy`가 없거나 `date`가 아니면 `lost_board_id` 기준(습득물 목록과 같은 규칙), `sortBy=date`는 그대로. `@Builder.Default` 14건(Member 5, Board 3, BoardDto 3, AcquiredBoard 1, AcquiredBoardDto 1, Agency 1 — no-args 생성자도 기본값 유지 확인), `JwtFilter`는 `PathPatternRequestMatcher`(제외 목록 그대로)
  - 확인: 컴파일 경고 27 → 0(`-Xlint`로 보면 이전부터 있던 deprecation·unchecked 12건: FCM, `new URL`, unchecked cast — R-23·R-26 대상 포함), 새 단위 테스트 4개(batch 호출 경로 `MockRestServiceServer`, 제외 목록 31건, 정렬 조건, 빌더 기본값) 통과, 부분 기동 회귀(로그인, 공개/인증 경로, `/losts` 정렬 SQL) 통과
  - 검증에서 Ant와 PathPattern 매처를 같은 요청 62개로 실제 비교: 끝 슬래시·`/**`·대소문자·확장자는 같고, `//losts`, `/./losts`, `/a/../losts` 3건만 새 매처가 더 엄격(셋 다 `StrictHttpFirewall`이 먼저 거부 → 보안 영향 없음)
- [x] **R-23** FCM 복구: 공식 문서(Firebase Admin SDK Java, FCM HTTP v1 웹푸시) 기준으로 firebase-admin 9.x 초기화·발송 코드 정비, `fcm.enabled` 조건부 초기화, `FCM_CREDENTIALS_PATH`, `secrets/` 마운트 (K-03). 완료 기준: 키 없이 기동하고 발송 단계는 오류 없이 건너뜀, 발송 로직 단위 테스트(Firebase 호출 없이 메시지 구성·토큰 조회·실패 처리) 통과. 실제 발송은 R-91 — 완료(2026-09-30, `feature/15-fcm`)
  - 결과: `Alarm/push/`(`PushSender` + `FcmPushSender`/`NoopPushSender`, `FcmConfig`, `@ConditionalOnFcm`), `NotificationService`는 알림 저장 후 `PushRequestedEvent` 발행 → `PushDispatchListener`가 **커밋 후** 발송, `UNREGISTERED`면 토큰 삭제(REQUIRES_NEW). `FCMInitializer`·`NotificationBodyDto` 삭제, 토큰 값 로그 제거, compose `./secrets:/run/secrets:ro`, `.env.example`에 `FCM_ENABLED`·`FCM_CREDENTIALS_PATH`. 공식 문서 기록은 05 §2·§8
  - 확인: Firebase 호출 없는 단위 테스트(메시지를 FCM v1 JSON으로 직렬화해 문서 형식과 비교, 조건부 빈, 가짜 트랜잭션 매니저로 커밋 전/후·롤백·트랜잭션 없음·삭제 실패), 부분 기동에서 쪽지 전송·답장 시 알림 저장 + 커밋 뒤 "FCM 비활성: 발송 건너뜀", `FCM_ENABLED=true`(또는 `yes`)+파일 없음이면 원인 메시지로 기동 실패, 잘못된 값(`maybe`)도 기동 실패
  - 진행: 첫 실행·검증 PASS 뒤, 검증 관찰(발송이 쪽지 트랜잭션 안에서 일어나 토큰 삭제 예외가 rollback-only를 만들 수 있음, FCM 대기 동안 DB 트랜잭션 점유, 토큰 로그, `fcm.enabled=yes`면 빈 0개)을 보완으로 처리. `@ConditionalOnBooleanProperty`(Boot 3.5)는 `havingValue`를 문자열로 비교해 `yes`에서 같은 문제가 남아 자체 조건으로 대체. 보완 검증 권고로 `PushMessage.toString`에서 토큰을 가리고, 테스트용 트랜잭션 매니저가 진행 중인 트랜잭션을 인식하게 고쳐 `REQUIRES_NEW`를 테스트가 지키게 함(`REQUIRED`로 바꾸면 2개 실패하는 것 확인)
  - 남은 참고: p6spy(local 전용) SQL 로그에는 토큰 값이 찍힘, FCM 호출은 요청 스레드에서 동기(커밋 후라 DB는 잡지 않음 — 지연을 더 줄이려면 `@Async` 검토)
- [x] **R-24** 스토리지: AWS SDK v2 `S3Client`(내부 엔드포인트) + `S3Presigner`(공개 엔드포인트), `POST /images/presign`, 게시글 등록 시 object key 저장·URL 조립 (D-13). 완료 기준: curl로 presign → PUT → 게시글 등록 → 조회 응답에 이미지 URL (presigned PUT은 여기서 처음 확인, D-42) — 완료(2026-09-30, `feature/15-image-storage`)
  - 결과: `POST /images/presign`(요청 `{contentType, contentLength}`, 응답 `{key, uploadUrl, url, expiresAt, headers}`), 등록·수정 요청은 `imgKeys`(형식·`HeadObject`·중복·다른 게시글 key 검증), 조회는 key → 공개 URL 조립(필드명 유지). V3(`img_key`, `thumbnail_key`, D-47), 시드 수정, compose `main`에 `STORAGE_*`·`AWS_*`·seaweedfs/storage-init 의존, `.env.example`에 공개 엔드포인트 등 4개. 계약은 07 §4 "이미지 업로드 계약", 설계·IAM 권한은 06 §4
  - 확인: 오프라인 presign 단위 테스트(로컬: `localhost:8333` path-style·`content-length;content-type;host` 서명·600초 / AWS 설정: `findear-images.s3.ap-northeast-2.amazonaws.com`), 부분 기동 end-to-end — presign → `curl PUT` 200 → 공개 GET 200(원본과 동일) → 습득물·분실물 등록 → 상세·목록 URL 200, DB엔 key만. 음성: 잘못된 타입·크기 400, 토큰 없음 401, 없는 key 400, 서명과 다른 Content-Type·크기·호스트로 PUT 403 (**presigned PUT 첫 확인, D-42**)
  - 함께 고친 것: **K-15**(분실물 목록의 쓰지 않는 이미지 조인 → 중복 행), **K-14**(수정 시 옛 이미지 행이 남음 → `ImgFileSync`로 정확히 요청 목록이 되게, 요청 DTO 내부 필드 `@JsonIgnore`), 수정 로직의 `orElse(save)` 즉시 저장 버그
  - 남은 참고: 공통 예외 핸들러가 HTTP 400에 본문 `status: 500`을 씀(R-27), 스토리지 고아 객체 정리·key 소유 검증은 1차 이후, `MainApplicationTests`는 R-27
- [ ] ~~**R-25**~~ **→ 1차 범위에서 제외, 추후 진행 (D-50, 2026-09-30)**. 원래 내용: Naver 로그인: 공식 문서(네이버 로그인 API 명세) 기준으로 인가 코드 → 토큰 교환 → 프로필 조회의 요청 파라미터·응답·오류 처리 점검·수정. (선택) 고정 `state` 개선. 완료 기준: 공식 문서의 응답 예시·오류 응답을 재현한 mock 서버 계약 테스트로 회원 조회/가입 → JWT 발급까지 통과, 키 미설정 시 "설정 필요" 오류 응답. 실제 로그인은 R-91
  - 지시서 초안에서 정한 것: 공식 명세와 대조해 틀린 곳만 수정 — 특히 프로필 응답의 고유 ID 필드(코드는 `uid`, 명세는 `id`로 보임 → 틀리면 `naver_uid NOT NULL`로 가입 실패), 토큰 오류 응답 처리, 프로필 조회 메서드. `after-login`에 `state` 선택 파라미터(없으면 기존 `test`). 키 미설정은 **D-49**(503). 계약 테스트는 mock HTTP 서버(MockWebServer 등), e2e는 레포 밖 임시 compose 파일 + mock 컨테이너 + relaxed binding 환경변수(`AUTH_NAVER_TOKENREQUESTURI` 등, 레포 yml에는 변수 추가 안 함)
  - 현재 코드에서 확인한 문제(R-25 착수 때, 공식 문서 대조 전): `NaverOAuthProvider.getMemberInfo`가 프로필 응답의 `response.get("uid")`를 읽음(제3자 자료 Ory·Logto·arctic·golang oauth2는 모두 고유 ID를 `response.id`로 적음 → 그대로면 `naver_uid NOT NULL`로 가입 실패), 예외를 삼키고 null을 돌려줘 호출부에서 NPE, 프로필을 본문 없는 POST + form Content-Type으로 조회, 토큰 응답의 `error`/`error_description`을 `afterSocialLogin` 경로에서 검사하지 않음, `refreshAccessToken`(`POST /members/token/refresh`)도 `getMemberInfo` 결과를 검사하지 않음, `state`가 상수 `test`, 키 미설정 처리 없음(D-49 미적용), Spring `AuthenticationServiceException`이 `CommonControllerAdvice`에서 401로 처리되지 않음(`javax.naming.AuthenticationException`을 잡고 있음). 추후 진행 때는 공식 문서를 사용자가 제공하거나 열람 가능한 방법으로 확인한 뒤 원래 지시서 초안(로컬 `.claude/work-orders/R-25.md`)대로
- [x] **R-26** VWorld: 공식 문서(검색 API 2.0, 주소→좌표 변환 API 2.0) 기준으로 `/location/search`, `/location/address`의 요청 파라미터·응답·오류 처리 점검·수정. 완료 기준: mock 서버 계약 테스트 통과, 키 미설정 시 "설정 필요" 오류 응답. 실제 호출은 R-91 — 완료(2026-09-30, `feature/15-vworld`)
  - 결과: `LocationController` 재작성(전용 `RestTemplate` 3s/5s + `UriComponentsBuilder`, 입력 검증, VWorld JSON 그대로, 키 든 URL 로그 안 남김), D-49 공통 코드(`ExternalServiceNotConfiguredException` → 503, `ExternalServiceUnavailableException` → 502, `ExternalServiceExceptionAdvice` 최우선). 공식 문서와 대조한 결과 기존 파라미터 값은 그대로(05 §5·§8)
  - 확인: `mockwebserver3` 계약 테스트 14개(요청 경로·파라미터·한 번 인코딩, OK/NOT_FOUND/ERROR 본문 그대로, 연결 불가·타임아웃 502, 키 없음 503·호출 없음, advice 우선순위 — `@Order`를 빼면 실패), e2e(WireMock + `VWORLD_BASEURL`)로 검색·주소 200·빈 query 400·mock 404 → 502, 기본 `.env`(키 없음)에서 503
  - 순서: 네이버 공식 문서를 열 수 없어 R-25보다 먼저 진행했고, R-25는 사용자 결정으로 1차에서 제외(D-50)
  - 참고: 테스트 전용 `mockwebserver3` 5.5.0은 Boot BOM이 관리하지 않아 버전을 명시했고, Boot BOM 때문에 테스트 클래스패스의 kotlin-stdlib가 1.9.25로 내려감(테스트는 정상). Git Bash `curl --data-urlencode`는 한글을 CP949로 보내므로 확인 명령에는 UTF-8 퍼센트 인코딩 URL을 쓸 것
- [x] **R-27** 보안 정리 (D-26): K-06(전화번호 로그인·테스트 가입), K-07(`/alarm/send-*`), K-12(`test-member-type` 헤더 인증 우회)를 local 프로필 한정, SSE 구독은 본인 ID만. (선택) Redis key serializer 개선, Testcontainers로 `MainApplicationTests` 복구 — 완료(2026-09-30, `feature/15-security-cleanup`)
  - 결과: 개발용 기능(전화번호 로그인·테스트 가입 K-06, `/alarm/send-*` K-07, `test-member-type` K-12, 회원 검색 `GET /members?keyword`)을 `LocalMemberController`·`LocalAlarmController`(`@Profile("local")`)와 `LocalProfile.isActive`로 local 전용, 공개 경로는 `security/PublicPaths` 한 곳에서 정의(`/alarm/**` permitAll·없는 경로 제거), SSE 구독은 본인만, 401·403·공통 예외를 D-51 규칙의 JSON으로(`CommonControllerAdvice` 재작성, `MemberControllerAdvice` 삭제), 게시글 수정·반환·롤백·쪽지 조회·답장·회원 수정·탈퇴·**role 변경(권한 상승 구멍)** 권한 검사, `Using generated security password` 경고 제거, 민감값 로그 정리, Redis 키 `refresh:{memberId}`, **`MainApplicationTests` Testcontainers 복구**(mysql:8.4.11·redis:8.8.3, 테스트에서만 Flyway로 `infra/db/migration`) → `./gradlew test` 전체 통과
  - 확인: 전체 테스트 177건 통과(local·prod 컨텍스트 통합 테스트 포함), local·prod 부분 기동으로 공개/인증/403/404·405, `refresh:*` 키·TTL, 기동 로그 확인
  - 실행 판단 승인: 알 수 없는 예외 500(D-51), 만료 토큰 401, SSE `produces` 제거(기본 JSON 협상 때문에 `*/*`에서 406), `checkSameAgency` 강화, prod 익명 `POST /members/login`은 401(토큰 있으면 405)

## Phase 3 — batch 복구

이슈 #17 (상위 #12). **완료 2026-10-01** — R-30~R-36 (R-31은 R-30과 한 브랜치). 결정 D-53~D-56. 작업 지시서 초안은 로컬 `.claude/work-orders/`(git 제외).

- [x] **R-30** Boot 3.5 마이그레이션 ([02 §3](02-current-state.md#3-팀-batch-old-serversbatch--복원-대상)): jakarta, Spring Batch 5 API, `@EnableBatchProcessing` 제거, `RestHighLevelClient` → Spring Data ES 5.5(`ElasticsearchOperations`), Spring Cloud 제거, 멀티스테이지 Dockerfile. 완료 기준: compose의 MySQL·ES에 붙어 기동, `validate` 통과 — 완료(2026-10-01, `feature/17-batch-boot35`, **R-31과 한 브랜치**: 팀 batch 설정은 Config Server에 있어 레포에 설정 파일이 없으므로 기동 확인에 설정 외부화가 먼저 필요)
  - 결과: Boot **3.5.16**, Gradle wrapper 8.14.5(main과 같은 스크립트·jar), Spring Cloud·mariadb·devtools 제거, `micrometer-registry-prometheus`, `batch/Dockerfile`(main과 같은 구성, `EXPOSE 8082 8083`). jakarta·Spring `@Transactional`·Batch 5 `JobBuilder`/`StepBuilder`, 스케줄러는 `Job` 빈 주입 + `JobParametersBuilder`(바쁜 대기 제거). ES는 Boot 자동 구성 + `ElasticsearchOperations`/`NativeQuery`(Spring Data ES 5.5.13, elasticsearch-java 8.18.8) — 조건·응답 JSON은 그대로, 전체 조회는 scroll(`searchForStream`), source Map 읽기 헬퍼 `common/elasticsearch/ElasticsearchSourceReader`. 엔티티는 V3에 맞춤(`thumbnailKey`, `imgKey`, `Member`에서 `password`·`alarmList` 제거). match 호출은 `RestTemplateBuilder` 빈 `matchRestTemplate`(연결 3s·읽기 30s, 원래 R-36 항목)
  - 옮기면서 바꾼 동작: 매칭 로그 목록(`/findear/board/{id}`, `/police/board/{id}`)은 ES 기본 size(10) 때문에 10건 안에서만 페이지를 자르던 것 → ES `from/size` + `totalCount`는 전체 일치 건수. Lost112 목록 날짜 범위는 `yyyy-MM-dd` 문자열(양끝 날짜 포함, 기간이 없으면 오늘까지 — 팀 코드는 KST 자정 epoch millis라 끝 날짜 당일이 빠질 수 있었음), 날짜 형식 오류는 예외. `/police/scrap`의 `id`는 문자열(옛 `(String)` 캐스트는 숫자 id에서 ClassCastException)
  - 삭제: `alarm/` 패키지 전체(주석 FCM 코드 + 쓰지 않는 `Alarm` 엔티티 — `generatedAt` 타입이 스키마와 달라 `validate`를 막음, 원래 R-36 항목)와 ES 7 `ElasticSearchConfig`. executor의 삭제가 자동 모드 분류기에 막혀 **사용자 승인 후 메인 세션이 `git rm`**
  - 확인: 테스트 39개(Testcontainers MySQL+Flyway·ES: `validate`, ES 이식 — 매칭 로그 12건 페이지·totalCount, scroll 1,100건, match 요청 본문을 계약 픽스처 키와 비교), 컴파일 경고 0. 부분 기동(`mysql flyway elasticsearch match batch`) healthy, 시드 분실물 1번 매칭 → 습득물 board 3, match 직접 호출과 같은 점수, ES에 넣은 Lost112 문서로 목록·카테고리·스크랩·Lost112 매칭, 관리 포트 비공개, `application="batch"`, `http_client_requests{client_name="match"}`, 512MiB·OOM 없음
  - 옮기기만 하고 고치지 않은 것(뒤 R-xx): 아래 R-33·R-34·R-35 메모
- [x] **R-31** 설정 외부화: match URL, Lost112 키·URL(https), cron·on/off, 관리 포트 8083 ([06 §6](06-db-and-config.md#batch)) — 완료(2026-10-01, R-30과 함께)
  - 결과: `application.yml`(모든 값 `${ENV:기본값}`, `MYSQL_PASSWORD` 기본값 없음, `spring.batch.job.enabled=false`·`jdbc.initialize-schema=never`, 관리 포트 8083) / `-local`(루트 `.env` import, 모드 B에서 `MYSQL_HOST_PORT`·`ES_HOST_PORT` 추종, SQL 로그) / `-prod`(아직 내용 없음). `servers.match-server.url`(`MATCH_SERVER_URL`), `lost112.service-key`(`LOST112_SERVICE_KEY`), `lost112.base-url`은 **환경변수 없이** 공식 주소(https) 고정(main의 외부 API 주소 규칙과 같음), `batch.scheduling.enabled`(`BATCH_SCHEDULING_ENABLED`, false면 스케줄러 빈 없음), `batch.jobs.{police,findear}.cron`(`POLICE_JOB_CRON`·`FINDEAR_JOB_CRON`, `.env.example`에는 공백이 든 값이라 주석 줄로만). compose `batch`(mysql·flyway·elasticsearch 의존, match 의존 없음, 512m), override `127.0.0.1:${BATCH_HOST_PORT:-8082}`(이 PC `.env`는 `8092`)
  - Lost112 수집 코드는 설정 이름만 바꾸고, 키가 비어 있으면 요청·기존 데이터 삭제 없이 중단하는 안전장치만 추가 (재작성은 R-32)
- [x] **R-32** Lost112 수집 개선 (공식 명세 기준: 공공데이터포털 활용가이드, D-38): 키 URL 인코딩, 페이지 단위 파싱 + bulk 인덱싱(512MB 제한 내 동작), 수집 기간 설정, `atcId` 문서 ID. 샘플 문서 적재 스크립트 `infra/elasticsearch/seed/` ([06 §8](06-db-and-config.md#8-시드더미-데이터)). 완료 기준: 명세서 응답 예시로 만든 XML 픽스처로 파싱·인덱싱 테스트 통과, mock 서버로 페이지 순회·오류 응답(키 오류, 트래픽 초과) 처리 테스트 통과, 샘플 적재 후 `GET /search/total` > 0. 실제 API 수집은 R-91 — 완료(2026-10-01, `feature/17-lost112-collect`)
  - 결과 (D-53): `police/client/`(`Lost112Client` — 서비스별 경로, 키 한 번 인코딩·Encoding 키도 받음, 빈 옵션 파라미터 안 보냄, 예외 메시지에 키·URL 없음 / `Lost112XmlParser` — XXE 방지, resultCode 00·03·그 밖, 게이트웨이 오류), `PoliceDataNormalizer`(fdYmd 두 형식, prdtClNm `>` 분리, clrNm 괄호 안 값·없으면 fdSbjt 추출), `Lost112CollectService`(최근 30일, 페이지마다 bulk upsert, 삭제 없음, 서비스별 실패 격리·요약), `POST /search/save` 200 요약·503·502(`Lost112ExceptionAdvice`), `police_acquired_data` 매핑 명시·문서 ID와 응답 `id` = atcId·목록 `fdYmd` desc 정렬·카테고리 term·스크랩 terms·null 안전 변환, 기존 매핑 불일치 WARN, 샘플 적재 `infra/elasticsearch/seed/`(가상 16건, 적재일 기준 날짜), `LOST112_COLLECT_DAYS`·`LOST112_PAGE_SIZE`
  - 확인: 테스트 89개(픽스처 11개 파싱·정규화, mockwebserver3 계약 19 — 원시 쿼리에서 `ab%2Bc%2Fd%3D%3D`, 페이지·종료·max-pages·서비스 실패 격리·로그에 키 없음, 인덱싱 — 2페이지 요청 시점에 1페이지 문서가 이미 ES에 있음, 재수집 upsert), deprecation 경고 0. e2e A: 매핑, 샘플 16건·정렬·카테고리·스크랩, 키 없음 503. e2e B(WireMock + relaxed binding `LOST112_BASEURL`): 두 서비스 2페이지씩, WireMock이 받은 쿼리 `serviceKey=test%2Bkey%2F%3D%3D&pageNo=1&numOfRows=2&START_YMD=20260901&END_YMD=20261001`, 재수집 수 유지, 게이트웨이 30·연결 실패 → 502, 로그에 키·`apis.data.go.kr` 0건. 검증은 `extra_hosts`로 `apis.data.go.kr`을 막고 수행
  - 다르게 한 것: `fdYmd`는 엔티티에서 `LocalDate`(String에 Date 매핑을 달면 Spring Data ES가 기동마다 WARN), `fdSn`·`source`는 응답에서 뺌, totalCount가 없으면 빈 페이지까지. 샘플의 `fdFilePathImg`는 `""`(필드가 없으면 Lost112 매칭 로그 저장의 null `toString()` NPE → R-33에서 로그 변환을 null 안전하게)
  - R-91에서 확인할 것: 05 §8 Lost112 행 (응답 구조 가정, numOfRows 최댓값, atcId 유일성, 트래픽)
  - 착수 전 명세 확인 (2026-10-01, [05 §8](05-external-integrations.md#8-공식-문서-확인-기록-d-38)): 팀 코드와 다른 점 — 경찰청 서비스의 색상 파라미터는 `FD_COL_CD`(포털기관은 `CLR_CD`), `prdtClNm` 구분자(`지갑 > 남성용 지갑` / `지갑>여성지갑`), `fdYmd` 형식(`2018-06-01` / `20110223`), 응답에 `clrNm`이 있음, 포털기관 개발계정 하루 10,000건. 요청·응답 XML 예시는 페이지에 없음
  - R-30 메모: Lost112 문서를 source Map으로 읽는 `convertToPoliceData`가 `fdSbjt`·`clrNm`이 null이면 생성자 인자가 한 칸씩 밀리고 다른 필드가 null이면 NPE(팀 코드 그대로 옮김), `atcId`가 동적 매핑으로 text라 `match` 쿼리가 분석됨, 매칭 후보 조회는 결과를 전부 메모리에 올림
- [x] **R-33** ES 매핑 명시([06 §3](06-db-and-config.md#3-elasticsearch-d-06)), 매칭 로그 결정적 ID — 완료(2026-10-01, `feature/17-matching-log-id`)
  - 결과 (D-54): 두 로그 인덱스 매핑 명시(`matchingAt` date `yyyy-MM-dd'T'HH:mm:ss` 초 단위, 엔티티는 `LocalDateTime`/`LocalDate` — 기동 WARN 0), 문서 ID `{lostBoardId}-{acquiredBoardId}`·`{lostBoardId}-{atcId}`(응답 ID 문자열), `MatchingLogWriter`가 로그를 쓰는 유일한 곳(분실물별 교체, null 안전 변환 — R-35의 "null `toString()`" 중 로그 저장 부분 해결), `count()+1` 제거, 기존 매핑 불일치 WARN을 `IndexMappingChecks`로 일반화
  - 확인: 테스트 102개(교체 규칙 — 결과와 같아짐·null/빈 목록 전부 삭제·유효 0건 유지·match 실패 유지·다른 분실물 불변, 동시성, 빈 인덱스 조회 200, 매핑, null 안전). e2e: 매칭 전 목록 4개 200 빈 목록, 시드 분실물 1 → findear `1-3`·police 4건, 재요청 수 유지·`matchedAt` 갱신, match 중지 시 오류(500, R-36)·로그 유지, 분실물 2 매칭 후에도 1의 로그 그대로
  - 진행 중 보완: 실행 담당은 "항목은 왔는데 유효 0건"이면 전부 삭제로 구현 → 메인 판단으로 기존 로그 유지로 바꿈
  - **R-34 전까지 주의**: 잡(`findearJob`, 2시간마다)이 아직 `acquiredBoardId`로 AcquiredBoard PK를 보내므로, R-33 교체 규칙 때문에 잡이 돌면 API가 만든 올바른 로그(board_id)를 PK 기반 로그로 바꿔 버린다 → R-34를 바로 이어서 진행
  - R-30 메모: Spring Data ES가 만드는 초기 매핑은 `_class`뿐이라 매칭 로그 인덱스가 비어 있으면 `/findear/board/{id}`·`/findear/member/{id}`·`/police/board/{id}`가 `similarityRate` 정렬 필드가 없어 500(팀 코드와 같음). 매칭 로그 ID `count()+1` → 같은 매칭을 다시 하면 로그가 중복으로 쌓임(e2e 확인). main은 `findearMatchingLogId`·`policeMatchingLogId`·`matchedAt`을 문자열·원본 그대로 넘기므로 ID를 문자열로 바꿔도 main 영향 없음
- [x] **R-34** 잡·스케줄 복원: `policeJob`(수집 on/off + Lost112 매칭), `findearJob`, 수동 트리거 유지. 완료 기준: 짧은 cron으로 두 잡 실행 → 매칭 로그 적재 — 완료(2026-10-01, `feature/17-batch-jobs`)
  - 결과 (D-55): `FindearMatchingService`·`PoliceMatchingService`(분실물 1건 매칭 — 등록 직후 API·두 잡 공용, `acquiredBoardId` = board_id, 후보에서 삭제·반환 완료 제외, 대상 분실물은 진행 중·삭제 안 됨), `LostBoardMatchingTasklet`(분실물 단위 실패 격리, 전부 실패면 스텝 FAILED, read/write/skip 카운트), `policeJob` = 수집(`LOST112_COLLECT_ENABLED`) → `on("*")` → Lost112 매칭(빈 태스크릿을 구현, 오타 이름 `PoliceDataMatcingTasklet` 정리), `BatchJobRunner`(스케줄러·API 공통, 같은 잡 동시 1개, 실행 요약 INFO 한 줄), `POST /findear/matching/batch`는 잡 실행으로·`POST /police/matching/batch` 추가(409, FAILED도 200 + 요약). `matchingFindearDatasBatch` 제거, 스케줄러 스레드 2, 잡 생성 격리 수준 `read_committed`
  - 확인: 테스트 124개. e2e(짧은 cron, 수집 off): 두 잡 COMPLETED(처리 2·성공 2), `/findear/board/1` → board 3, `/findear/board/2` → board 4(board_id), Lost112 매칭 지갑 4·전자기기 3건, 메타 테이블 기록, 수동 API 200·동시 요청 409, match 중지 → findearJob FAILED(분실물마다 WARN 한 줄, 스택 없음) → 재기동 후 COMPLETED, `spring_batch_job_seconds_*`·`spring_batch_job_launch_count_total` 노출. 두 잡이 매번 같은 초에 시작하는 cron으로 90초 → 교착 0, COMPLETED 7·7
  - 진행 중 보완: 실행 담당이 e2e에서 두 잡 동시 시작 시 `BATCH_JOB_INSTANCE` 교착을 발견해 재시도를 넣었으나, 검증에서 동기 런처라 재시도가 잡 전체를 다시 돌릴 수 있다는 지적 → 재시도를 빼고 격리 수준을 `read_committed`로
  - 참고 (뒤 작업): 수집 실패는 `policeSaveStep` ABANDONED(EXIT_CODE FAILED)로 남고 잡은 COMPLETED → 잡 단위 지표로는 안 보임(R-50), 기동 시 Micrometer WARN 1회(`spring.batch.job.active` 태그 충돌 — `spring_batch_job_active_seconds`는 `active_name` 태그 쪽만, R-50), 등록 직후 API와 잡이 같은 분실물을 동시에 매칭하면 한쪽의 stale 삭제가 다른 쪽 결과를 지울 수 있음(다음 잡 실행에서 복구), `Board` 역방향 OneToOne 때문에 행마다 추가 select(N+1), 분실물마다 같은 카테고리 Lost112 후보를 다시 읽음, 스텝 동안 DB 커넥션을 match 호출 내내 잡음
  - R-30 메모 (팀 코드 그대로 옮긴 버그): `FindearDataMatchingTasklet`(`findearJob`)과 `matchingFindearDatasBatch`(`POST /findear/matching/batch`)는 match에 `acquiredBoardId`로 **`AcquiredBoard` PK**를 보냄 — main은 이 값을 **board_id**로 읽음(`findByBoardId`), 분실물 등록 직후 매칭(`matchingFindearDatas`)은 board_id를 보냄. `matchingFindearDatasBatch`는 결과가 없는 분실물 하나에서 전체를 `return`. `PoliceDataMatcingTasklet`(`policeJob`의 유일한 스텝)은 빈 구현이라 정기 Lost112 매칭이 없음. 분실물·습득물 조회에 삭제(`deleteYn`) 필터 없음
- [x] **R-35** main↔batch 계약 검증 ([07 §2](07-api-contracts.md#2-main--batch)): 분실물 등록 → 매칭 → 매칭 목록 조회, Lost112 목록·스크랩 end-to-end — 완료(2026-10-01, `fix/17-main-batch-contract`)
  - 결과: main `LostBoardCommandServiceImpl.register`는 `LostBoardMatchingRequestedEvent`만 발행 → `LostBoardMatchingRequestListener`(커밋 후) → `BatchMatchingClient`(Boot `WebClient.Builder`, `servers.batch-server.matching-timeout` 60s, 실패 WARN 한 줄) → `LostBoardMatchingAlertService`(findear·police 중 1건 이상이면 이벤트의 분실물 id로 작성자에게 알림 1건, 삭제된 분실물은 건너뜀). Lost112 목록 쿼리 값을 URI 변수로 엄격히 인코딩(K-01 남은 점 해결). batch는 수정할 것 없음
  - 확인: main 테스트 235개(커밋 후 호출·롤백 — `@EventListener`로 바꾸면 3건 실패, 요청 본문 키 8개·`lostAt` 형식, 응답 모양별 알림, 인코딩 — 엄격하지 않은 인코딩으로 바꾸면 실패). e2e(`mysql flyway redis seaweedfs storage-init elasticsearch match batch main` + Lost112 샘플): 분실물 등록 응답 0.27~0.44s(batch 처리가 응답 뒤에 이어짐) → main `/matchings/findear/bests`에 시드 습득물(board 3, main이 습득물 정보를 채움), `/matchings/lost112/bests`·`total`, 알림 1건씩(FCM 비활성 건너뜀 로그), Lost112 목록(id = atcId, 최신순)·`total-page`, Lost112 스크랩 → `/acquisitions/scraps`, `keyword=a%2Bb` → batch `a+b`, 한글 category·keyword 한 번만 인코딩, batch 중지 시 등록 200·WARN 한 줄, ERROR 0
  - 참고: 시드 습득물은 `registered_at`이 오늘-2일이라 `lostAt`이 그보다 늦은 분실물과는 Findear 매칭이 안 됨(후보 조건 `registeredAt >= lostAt`, 의도된 동작) — R-90 시나리오에서 분실일을 오늘-3일 이전으로. batch가 죽어 있으면 main WARN은 reactor-netty 연결 시간 초과(30s) 뒤에 나옴(R-50의 연결 시간 제한 메모와 같은 문제). `MatchAutoFillClientTest`는 클라이언트 timeout 300ms 고정이라 첫 요청이 느린 환경에서 흔들릴 수 있음(R-60 CI 때 확인). `findAllInLost112`의 `printStackTrace`·객체 해시 로그는 남음
  - R-40·R-41 메모: main `LostBoardCommandServiceImpl.register`의 batch `/findear/matching` 요청도 등록 트랜잭션 안에서 `subscribe`함(R-41과 같은 구조, 응답 콜백이 `lostBoardQueryRepository.findById(...).get()`) → R-41과 같은 방식(커밋 후 이벤트, Builder 빈)으로 정리. batch 팀 코드는 match `/matching/lost` 결과의 `atcId`·`fdFilePathImg` 등을 null 검사 없이 `toString()` → null 방어. ~~batch가 match에 `xpos`/`ypos`로 보내는지 확인~~ → R-30 계약 테스트로 확인됨
  - R-30 메모: batch `/findear/matching`의 `lostAt`은 날짜(`yyyy-MM-dd`)만 받음(시각이 붙으면 `FindearException`) — main이 보내는 형식 확인
- [x] **R-36** 정리: ~~주석 처리된 FCM·alarm 코드 삭제~~(R-30에서 함), 위험 엔드포인트 local 한정 ([07 §3](07-api-contracts.md#3-batch-api-전체-팀-버전와-1차-처리)), ~~`new RestTemplate()` → 빈~~(R-30에서 함), `System.out`·`printStackTrace` 정리, 오류 응답 형식(batch에는 공통 예외 처리가 없어 Spring 기본 오류 JSON) — 완료(2026-10-01, `feature/17-batch-cleanup`)
  - 결과 (D-56): 테스트 엔드포인트 3종 삭제, 전체 조회·삭제·`GET /search/save`는 `@Profile("local")` 컨트롤러 3개, `CommonControllerAdvice`(404·400·502·405·415·500, `{status, message}`), 입력 검사 `common/request/RequestChecks`, 서비스의 `FindearException`·`PoliceException` 래핑 제거(match 호출 실패는 원인을 보존한 `MatchServerException`), `System.out`·`printStackTrace` 0건, 쓰지 않는 클래스 5개 삭제(실행 담당 보고 → 메인이 참조 0건 확인 후 `git rm`)
  - 확인: 테스트 143개(오류 응답 9, local·prod 프로필별 엔드포인트 5·5). e2e local·prod(`SPRING_PROFILES_ACTIVE=prod`): 프로필별 404/405, 없는 분실물 404·잘못된 입력 400·match 중지 502(로그 유지), 정기 잡 분실물 단위 격리 유지. main이 batch 오류를 받는 경로는 모두 자기 오류로 바꿔 사용자에게 보이는 결과는 이전과 같음(검증에서 코드 확인)
  - 참고: 분류(category) 없는 분실물은 등록 직후 매칭이 400으로 끝남(main은 WARN만, 사용자 영향 없음), ES 장애는 500, `GET /search`의 `page*size` > 10,000이면 ES 한도로 500, 404/405 메시지는 영문 상태 설명(main과 같음)

## Phase 4 — match mock (Phase 2와 병렬 가능)

이슈 #16 (상위 #12). **완료 2026-09-30** — R-40·R-41. R-40 착수 때 main 쪽 자동채움 호출 문제를 발견해 R-41을 추가함. batch와의 실제 연동은 batch가 복구된 뒤 R-34·R-35에서 확인 (batch는 아직 팀 버전 코드라 기동 불가) — 이 Phase에서는 batch가 보내는 요청 모양을 재현한 JSON 픽스처로 계약을 검증.

- [x] **R-40** `match/` 신규 Spring Boot 3.5 앱: [07 §5](07-api-contracts.md#5-match-mock-동작-명세-r-40-d-28) 명세대로 3개 API, `MatchingScorer` 인터페이스, 관리 포트 8085, Dockerfile. 완료 기준: JSON 픽스처 계약 테스트 통과, match 부분 기동(healthy·픽스처 요청 200·관리 포트 비공개). main 연동은 R-41, batch 연동은 R-34·R-35 — 완료(2026-09-30, `feature/16-match-mock`)
  - 결과: `match/`(Boot 3.5.16, record DTO, Lombok·DB 없음), `/process`(SHA-256 결정적 카테고리·색상, 키워드 항상 5개), `/matching/findear`·`/matching/lost`(`MatchingScorer` + 기본 `DeterministicMatchingScorer`, 자르기·반올림·안정 정렬·상한은 서비스 공통), 오류 응답 `{"message"}` 한 모양, 지연·설정 범위 검증, compose `match`(256m, 의존 없음)·override(`127.0.0.1:${MATCH_HOST_PORT:-8084}`)·`.env.example`. 세부는 07 §5
  - 확인: 테스트 44개(main·batch가 보내는 모양의 픽스처, 기대 점수는 테스트에서 명세 공식으로 따로 계산), match만 부분 기동 약 10초에 healthy, 픽스처 3종 200·같은 요청 같은 응답·한글 정상, 관리 포트는 컨테이너 안에서만(`application="match"` 지표), 256MiB·OOM 없음·uid 10001, 지연 500ms 적용. 검증에서 응답값을 Python으로 따로 계산해 일치 확인
  - 실행 판단 승인: 기본 scorer는 `@AutoConfiguration`(+ `AutoConfiguration.imports`)에서 `@ConditionalOnMissingBean`으로 등록 (일반 `@Configuration`이면 사용자 빈과 함께 두 개가 되는 것을 테스트로 확인), 406·그 밖의 MVC 4xx도 `{"message"}`
  - 참고: 팀 시절 404 실패 흉내(`GPT api failed`)는 만들지 않음. batch 팀 코드는 Lost112 결과 필드를 null 검사 없이 `toString()` → R-35
- [x] **R-41** main 습득물 자동채움 연동 정비 (R-40 착수 때 발견, D-52): `AcquiredBoardCommandServiceImpl.register`가 트랜잭션 **커밋 전에** WebClient로 match `/process`를 비동기 호출하고, 응답 콜백이 다른 스레드에서 등록 시점 엔티티를 통째로 `save`(merge)함 → mock이 즉시 응답하면 커밋 전 게시글을 merge(실패·중복 위험), 응답 전에 관리자가 수정하면 수정이 되돌아감, 롤백돼도 요청이 나감, `description`이 비면 예외, 대기 시간 제한 없음. 바꿀 것: 커밋 후 이벤트(`@TransactionalEventListener(AFTER_COMMIT)`)로 호출, Boot `WebClient.Builder` 빈(K-09)·타임아웃, 응답은 새 트랜잭션에서 게시글을 다시 읽어 **비어 있는 컬럼만** 채움. 완료 기준: 커밋 후 호출·롤백 시 미호출 테스트, mockwebserver3 계약 테스트(요청 모양, 404·500·시간 초과), 반영 로직 테스트, 부분 기동 e2e(등록 → mock 값으로 채워짐·match 직접 호출과 같은 값, 지연 중 수정은 보존, match 중지 시 WARN 한 줄) — 완료(2026-09-30, `fix/16-autofill-after-commit`)
  - 결과: `register`는 `AutoFillRequestedEvent`만 발행 → `AutoFillRequestListener`(`@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)`) → `MatchAutoFillClient`(Boot `WebClient.Builder`, `servers.match-server.autofill-timeout` 30s, 반영은 `boundedElastic`) → `AcquiredBoardAutoFillService.apply`(새 트랜잭션, `findByIdAndDeleteYnFalse` → `Board.fillAutoColumns`, 더티 체킹). `Board.updateAutofillColumn`·`AcquiredBoard.updateAutoFilledColumn` 삭제, `NotFilledBoardDto`는 record(JSON 키 그대로)
  - 확인: main 전체 테스트 203건 통과(새 26건: 리스너 5, 클라이언트 계약 10, 채움 규칙 7, Testcontainers 반영 4). 검증에서 변이 시험 — 리스너를 `@EventListener`로 바꾸면 3건, `apply`의 `@Transactional`을 지우면 2건 실패. e2e(mysql·flyway·redis·seaweedfs·storage-init·main·match 부분 기동): 등록 → 수 초 안에 mock 값으로 채워지고 match 직접 호출 결과와 같음, `MATCH_MOCK_LATENCY_MS=5000` 동안 `PATCH` 한 category는 유지·나머지만 채움, match 중지 시 등록 200(0.05초)·게시글 null·WARN 한 줄(스택 없음), 그 밖의 ERROR 없음
  - 참고: match를 **멈추면** WARN이 약 30초(타임아웃) 뒤에 찍힘 — 사라진 컨테이너 이름을 Docker 내장 DNS가 호스트 리졸버로 넘겨 조회에 약 8초씩 걸리고(`getent hosts match` 7.9초, 있는 이름은 2ms) Reactor Netty 해석이 재시도하다 타임아웃에 걸림. 동작 기준(비차단·null·WARN 한 줄)은 충족, 빨리 알아야 하면 R-50에서 연결·해석 시간 제한 검토. `publishOn(boundedElastic)`을 빼도 실패하는 테스트는 없음(코드·e2e 로그 스레드명으로 확인)

## Phase 5 — 모니터링 연결

이슈 #18 (상위 #12). **완료 2026-10-01** — R-50·R-51. 결정 D-57.

- [x] **R-50** 앱 지표: 3개 앱 actuator/prometheus, `application` 태그, WebClient·RestTemplate을 Builder 빈으로(K-09), 커스텀 지표([04 §6](04-target-architecture.md#6-모니터링-설계-d-18)) — 완료(2026-10-01, `feature/18-app-metrics`)
  - 결과: `prometheus.yml`에 job `main`·`batch`·`match`(`/actuator/prometheus`). main 시간 제한 `spring.http.client.{connect,read}-timeout` 3s/10s·`spring.http.reactiveclient.connect-timeout` 3s(VWorld 전용 3s/5s 유지) → 상대 컨테이너가 멈췄을 때 실패 감지 match 30.0s→3.0s, batch 14.4s→3.0s(이름 해석 자체가 안 되는 경우 약 4s는 Docker 내장 DNS 지연). 커스텀 지표 3종(04 §6). main→batch 호출은 batch 전용 RestTemplate + URI 템플릿(uri 태그 고카디널리티 해결, R-35 인코딩 유지). batch `MeterFilter`로 Spring Batch 5.2 지표 중복 WARN 제거(spring-batch#4753 — **기동 때가 아니라 첫 잡 실행 때** 나던 것)
  - 확인: main 테스트 248·batch 151, promtool SUCCESS. 부분 기동(`… main batch match prometheus`)에서 앱 타깃 UP, `http_server_requests`·`http_client_requests`(main→match·batch, batch→match)·`spring_batch_*`·`findear_*`(0으로 존재, FCM skipped 1)·JVM 지표 질의, uri 태그에 id·검색어·`none` 0건, batch 로그 Micrometer WARN 0
  - 진행 중 보완: 실행 결과에서 main→batch 호출의 uri 태그가 `/findear/member/1?page=1&size=6`·`none`인 것을 메인이 보고 batch 전용 RestTemplate + 템플릿으로 바꾸게 함. 그 과정에서 `lombok.config`로 `@Qualifier`를 복사하는 방식이 Docker 빌드에서 빠지는 것을 e2e로 발견 → 명시 생성자로
  - 참고: `POST /acquisitions`·`POST /losts` 응답이 매번 약 2.1초(R-50 전후 같음, 원인 미조사 — R-90에서 확인), R-34 메모의 "기동 때 WARN"은 첫 잡 실행 때로 정정
  - R-40·R-41 메모: match는 관리 포트 8085에 `/actuator/prometheus`(`application="match"`)가 이미 있음 → Prometheus job만 추가. 습득물 자동채움 WebClient는 R-41에서 Builder 빈으로 바꿈(`http_client_requests_*` 노출은 아직 확인 안 함). match가 없을 때 실패를 늦게(30s) 아는 문제 → WebClient 연결·DNS 해석 시간 제한 검토
  - R-34 메모: Spring Batch 잡 지표 확인됨(`spring_batch_job_seconds_*{spring_batch_job_name,spring_batch_job_status}`, `spring_batch_job_launch_count_total`). 기동 때 Micrometer WARN 1회 — `spring.batch.job.active` 태그 키 충돌로 `spring_batch_job_active_seconds`는 `spring_batch_job_active_name` 태그 쪽만 노출. policeJob은 수집이 실패해도 COMPLETED라 수집 실패는 스텝 지표(`spring_batch_step_*`, exit code)로 봐야 함
  - R-30 메모: batch도 관리 포트 8083에 `/actuator/prometheus`(`application="batch"`)가 있고 match 호출 `http_client_requests_*{client_name="match"}`가 잡히는 것을 확인 → Prometheus job 추가. Spring Batch 잡 지표(`spring_batch_job_*`)는 잡을 실행하지 않아 아직 확인 안 함
- [x] **R-51** Grafana 대시보드: 후보 ID 대시보드 JSON 커밋 + "Findear Overview" 작성. 완료 기준: 모니터링 + 대상 일부(예: main과 그 의존 서비스)만 부분 기동해 해당 타깃 UP, 관련 패널에 데이터 표시. 전체 타깃 동시 확인과 메모리 실측은 R-90에서 (D-32) — 완료(2026-10-01, `feature/18-grafana-dashboards`)
  - 결과: provisioning(`provisioning/dashboards/findear.yml`, compose grafana에 `dashboards` 마운트), 가져온 대시보드 6개(4701, 19004, 7362, 763, 14191, 14282 — 고른 이유·손댄 곳은 04 §6과 `dashboards/README.md`), Findear Overview(행 8·패널 25), 앱 HTTP 히스토그램(p95용). Grafana 메모리 기본값 192m → 512m(D-57)
  - 확인: 앱 테스트(main 248·batch 151·match 44), 묶음 A(main 쪽 + 모니터링)·B(batch·match·ES 쪽 + 모니터링)로 나눠 부분 기동하고 모든 패널 쿼리를 Grafana `/api/ds/query`로 실행 — 값 있음/정상 없음(그 묶음에 없는 대상, exporter가 내지 않는 지표)/문제로 분류, headless Chrome 스크린샷. Overview B: 잡 실행 findear·police COMPLETED, 스텝 비정상 0, Lost112 8조합 0, ES 문서·클러스터 상태, batch→match p95
  - 검증 FAIL 1건 → 메인이 수정: "메모리 제한 대비 비율" 패널이 `on (name)` 조인이라 컨테이너 재생성 직후 약 5분(cAdvisor가 옛 컨테이너 시계열을 같은 이름으로 남김) 쿼리 오류 → `on (id, name)`. 재생성 직후 옛 쿼리 422·새 쿼리 정상을 직접 확인
  - Grafana OOM: 실행 중 192m에서 브라우저로 대시보드를 열자 OOMKilled → 검증에서 256m(04 §5 "여유")·384m도 OOM, 512m만 통과 → 공식 최소 권장 512MB로 기본값 변경(D-57). 이 PC 로컬 `.env`의 `GRAFANA_MEM_LIMIT`도 512m로
  - 하지 않은 것(기본값 유지): Tomcat `server.tomcat.mbeanregistry.enabled`(JVM 4701 Utilisation 패널), es-exporter `--es.indices`(ES Indices 행)

## Phase 6 — 배포 준비 (P5, [09](09-deploy-and-aws.md))

이슈 #19 (상위 #12). 진행중 — 방향은 D-58.

- [x] **R-60** `.github/workflows/ci.yml`: PR·push 시 main/batch/match 빌드·테스트 (U-03 — 켜져 있음, D-58: CI는 자동, 아무것도 올리지 않음) — 완료(2026-10-01, `feature/19-ci`)
  - 결과: 모듈 matrix(`fail-fast: false`) + JDK 17(temurin) + `gradle/actions/setup-gradle`(캐시 `cache-provider: basic`, D-59) → `./gradlew build`. 트리거 push(모든 브랜치)·PR·수동, `docs/**`·`front/**`·`**/*.md`만 바뀐 커밋은 건너뜀, 같은 ref 이전 실행 취소, `permissions: contents: read`. 실패 테스트의 예외 전체는 CI 전용 init 스크립트 `.github/ci/test-logging.gradle`로 로그에 출력(산출물 업로드 없음)
  - 확인: actionlint 1.7.12 통과, init 스크립트 적용(`exceptionFormat` FULL) 확인. GitHub 첫 실행(run 36832918839, 작업 브랜치 push) 성공 — main 약 3분·batch 약 4분(Testcontainers 동작)·match 41초. `MatchAutoFillClientTest`(300ms) 통과
  - 검증에서 발견: `setup-gradle` v6 기본 캐시(`enhanced`)는 상용 구성요소라 이용약관 동의가 따름 → `basic`(MIT)으로 (D-59)
  - R-27 메모: main 테스트의 `MainApplicationTests`·보안 통합 테스트는 Testcontainers(Docker)를 쓴다 — GitHub Actions ubuntu 러너는 Docker가 있어 그대로 동작. 전체 `./gradlew test` 약 2분(로컬)
- [x] **R-61** `.github/workflows/images.yml`: ~~master push 시~~ **수동 실행(`workflow_dispatch`)으로만**(D-58) GHCR 이미지 빌드·푸시(`ghcr.io/ehighg/findear-{main,batch,match}`, 태그 전체 커밋 SHA·`latest`). 실제 업로드와 패키지 visibility 확인은 사용자가 배포를 결정할 때 — 1차 작업에서는 워크플로 작성과 로컬 문법 검사(actionlint)까지. 로컬 `compose.override.yml`의 앱 서비스에 `pull_policy: build`(D-58) — 완료(2026-10-01, `feature/19-ghcr-images`)
  - 결과: master에서만 실행(다른 브랜치는 job 건너뜀), 입력 `platforms`(`linux/amd64` 기본 / `+linux/arm64`면 QEMU), 모듈 matrix, `docker/metadata-action`(이미지 이름 소문자 변환, OCI `source` 라벨로 패키지가 레포에 연결) → 태그 `latest`·전체 40자 SHA(접두사 없음, 롤백 때 `IMAGE_TAG`에 그대로), BuildKit 캐시 `type=gha`(모듈별 scope), 권한 `contents: read`·`packages: write`만, 동시 실행은 대기
  - 확인: actionlint 통과. match 부분 기동으로 `docker compose pull match` → `Skipped`(GHCR에서 받지 않음), `up -d`마다 다시 빌드(레이어 CACHED)하고 컨테이너를 다시 만드는 것 확인 — Compose 문서의 `build` 설명과 같음, 04 §4에 기록. 워크플로 실행·GHCR 푸시는 하지 않음
- [ ] **R-62** `compose.prod.yml`: GHCR 이미지, main `80:8080`, Redis·ES 비밀번호/보안 on, 모니터링 127.0.0.1 바인딩, node-exporter, `restart`, 로그 로테이션. 완료 기준: `docker compose -f compose.yml -f compose.prod.yml config --quiet` 통과. 배포 서버에서의 실행 확인은 생략 (D-41)
  - R-51 메모: 배포용 대시보드 Node Exporter Full 1860 추가, MySQL 7362의 node-exporter 패널 7개가 채워지는지, 리눅스에서는 cAdvisor 19792(파일시스템·CFS 지표) 재검토, 배포 Grafana 메모리(512m, D-57)
  - R-24 메모: `compose.yml`의 `main`은 `STORAGE_ENDPOINT: http://seaweedfs:8333`과 `depends_on: seaweedfs, storage-init`을 가짐 → AWS S3로 배포하면 `compose.prod.yml`에서 엔드포인트를 비우고(`STORAGE_PUBLIC_ENDPOINT`도 빈 값 — compose가 `${…-기본값}`이라 빈 값이 유지됨) seaweedfs 의존·서비스를 빼는 구성을 둔다
  - R-27 메모: `compose.yml`의 `SPRING_PROFILES_ACTIVE` 기본값이 `local`이라 배포에서 변수를 빠뜨리거나 `local,prod`로 두면 개발용 기능(D-26)이 열림 → `compose.prod.yml`에서 `prod`를 명시하고, `local`과 `prod`가 함께 켜지면 main 기동을 실패시키는 가드를 검토
  - R-30 메모: batch의 ES 인증(`spring.elasticsearch.username`/`password`, `ELASTIC_PASSWORD`)은 아직 연결하지 않음(로컬 ES는 보안 off) → 배포에서 ES 보안을 켤 때 batch·es-exporter에 함께 넣는다
- [ ] **R-63** `infra/deploy/init-host.sh`(Ubuntu: Docker, `vm.max_map_count`, swap), `deploy.sh`(pull → up, `IMAGE_TAG` 롤백, `.env`의 비밀값이 `.env.example` 예시값 그대로면(`JWT_SECRET`, DB·Grafana 비밀번호 등) 멈춤). 완료 기준: `bash -n` 통과. EC2에서의 실행 확인은 생략 (D-41)
- [ ] **R-64** AWS S3 연동 키트 `infra/aws/` ([09 §4](09-deploy-and-aws.md#4-aws-s3-연동-키트)). AWS 공식 문서(CLI `s3api`, IAM) 기준으로 작성하고 AWS는 호출하지 않음 (D-38). 완료 기준: 스크립트 `bash -n` 통과, 정책 JSON 문법 검사 통과, 같은 버킷·CORS 명령이 로컬 SeaweedFS(`storage-init`)에서 동작. AWS 전용 부분(Public Access Block, 버킷 정책, IAM)은 문법 검사까지 (D-41)
  - R-24 메모: 서버(EC2) Role 정책에 `s3:PutObject`(`images/*`, presigned PUT 서명용), `s3:GetObject`(`HeadObject`), `s3:ListBucket`(없으면 없는 객체가 403) 포함 (06 §4)
- [ ] **R-65** (선택) `deploy.yml`: workflow_dispatch로 SSH 배포 (시크릿 이름만 문서화). 실행 확인은 생략 (D-41)

## Phase 7 — 검증 도구

- [ ] **R-80** `tools/fcm-test/`: 공식 문서(Firebase JS SDK 웹 메시징) 기준 `index.html` + `firebase-messaging-sw.js` + `firebase-config.example.js`, `python3 -m http.server 5500 -d tools/fcm-test`로 실행 (로컬 Windows PC에서는 `python3`가 스토어 별칭이라 `python`). 흐름: 테스트 로그인으로 JWT → 알림 권한 → `getToken(VAPID)` → `POST /notification/new` → `POST /alarm/send-fcm/{memberId}`. 완료 기준: `firebase-config.js`가 없으면 Firebase를 초기화하지 않고 설정 안내만 표시하는 것까지 확인. 토큰 발급·알림 수신은 R-91
- [ ] **R-81** 외부 연동 키 세팅 가이드·확인 스크립트 (D-38): [05](05-external-integrations.md)에 "키 세팅 체크리스트"(연동별로 채울 `.env` 변수·`secrets/` 파일·콘솔 설정값), `tools/verify-external/`(README + `verify.sh`: `.env`·`secrets/` 누락 검사 → 설정된 연동만 main·batch 엔드포인트를 거쳐 확인 요청 → 결과 요약). 완료 기준: 키가 없는 지금 상태에서 실행하면 외부 호출 없이 "미설정" 항목만 보고하고 끝남, `bash -n` 통과. 외부 호출 경로는 R-91에서 사용자가 실행

## Phase 8 — 1차 목표 최종 검증

- [ ] **R-90** 아래 시나리오를 처음부터 끝까지 수행하고 결과를 [10-worklog.md](10-worklog.md)에 기록. **이 단계에서는 모니터링까지 전체를 한 번에 띄운다 (D-32).** 외부 키는 비워 둔 상태로 진행하고 외부 API는 호출하지 않는다 (D-38) — 외부 연동의 실제 동작은 R-91.
  1. 깨끗한 clone → `cp .env.example .env`(외부 키 제외한 값 채움, 이 PC는 `*_HOST_PORT`도) → `docker compose config --quiet`, `docker compose -f compose.yml -f compose.prod.yml config --quiet` 통과, `.env.example`이 [06 §6](06-db-and-config.md#6-환경변수-전체-목록)과 일치 (D-43) → `docker compose up -d --build`(`COMPOSE_PROFILES=monitoring`) → `docker compose ps`: 상시 서비스와 모니터링 서비스 전부 healthy(헬스체크가 없는 redis-exporter는 running), `flyway`·`storage-init`은 exit 0
  2. 테스트 로그인(local): `POST /members/login` `{"phoneNumber": "<시드 회원 번호>"}` → accessToken 획득
  3. 이미지: `POST /images/presign` → `curl -X PUT --upload-file a.jpg -H 'Content-Type: image/jpeg' "<uploadUrl>"` → 응답의 `url`로 GET 200
  4. 습득물 등록(MANAGER 회원): `POST /acquisitions`(이미지 key 포함) → 잠시 후 `GET /acquisitions/{boardId}`에 mock이 채운 category·color·description
  5. 분실물 등록(NORMAL 회원): `POST /losts` → batch `/findear/matching` → match mock → `GET /matchings/findear/bests`에 결과. FCM 비활성 상태에서 알림 단계가 오류 없이 건너뛰어짐. 분실일은 **오늘-3일 이전**(시드 습득물 `registered_at`이 오늘-2일이라 그보다 늦은 분실일이면 Findear 후보가 없음, R-35)
  6. Lost112: 샘플 문서 적재(`infra/elasticsearch/seed/`) → main `GET /acquisitions/lost112?…` 목록과 `GET /acquisitions/lost112/total-page`
  7. 배치 잡: `FINDEAR_JOB_CRON`·`POLICE_JOB_CRON`을 짧게(Lost112 수집은 off) → 매칭 로그 증가, `GET /matchings/lost112/bests`
  8. 쪽지: `POST /message`, `POST /message/reply` → 목록 조회 (FCM 비활성 상태에서 오류 없음)
  9. 외부 연동 미설정 상태 점검: `tools/verify-external/verify.sh` → 외부 호출 없이 미설정 항목 보고. VWorld 엔드포인트는 "설정 필요"(503) 오류 응답 (Naver는 D-50으로 제외)
  10. 모니터링: `http://localhost:9090/targets` 전부 UP, Grafana "Findear Overview"와 가져온 대시보드(JVM, MySQL, Redis, ES, cAdvisor) 패널에 데이터 — R-51의 패널 검사 방식(`/api/ds/query`로 모든 패널 실행, 확인 트래픽은 15초 이상 간격). 전체 기동에서만 보이는 조합(main→batch·match 호출, FCM, batch 재시작 뒤 잡 패널)과 전체 시계열 수(히스토그램 버킷)를 기록
  11. 자원 실측: 2~10을 수행한 뒤 `docker stats --no-stream`으로 컨테이너별 메모리·CPU 기록, OOM 여부(`docker inspect -f '{{.State.OOMKilled}}'`) 확인 → [04 §5](04-target-architecture.md#5-리소스-산정-메모리) 표 갱신(부족한 서비스는 "여유" 값으로). 비밀값 커밋 여부 최종 확인 (위 "비밀값 검사")
  - 완료 기준: 1~11 통과 → [README](README.md#3-1차-목표-완료-기준-definition-of-done)의 DoD 충족. 여기까지가 Claude의 1차 작업

## 1차 작업 완료 후 — 사용자 (키 세팅)

- [ ] **R-91** 키 세팅 후 외부 연동 확인 (사용자, U-01·U-04~U-07 후): [05](05-external-integrations.md)의 "키 세팅 체크리스트"대로 `.env`·`secrets/`·`tools/fcm-test/firebase-config.js`를 채우고 `docker compose up -d` → `tools/verify-external/verify.sh`
  1. ~~Naver 로그인~~ — 1차에서 제외 (D-50). 추후 R-25를 진행한 뒤 확인: 브라우저 authorize → 콜백 code → `GET /members/after-login?code=…`로 JWT
  2. VWorld: `GET /location/search?query=서울역&page=1&size=5`, `GET /location/address?…`
  3. Lost112: `LOST112_COLLECT_ENABLED=true` → batch `POST /search/save`(또는 짧은 cron) → main `GET /acquisitions/lost112` 목록
  4. FCM: `FCM_ENABLED=true` → R-80 테스트 페이지에서 토큰 등록 → `POST /alarm/send-fcm/{memberId}` 알림 수신 → 분실물 등록 매칭 알림, 쪽지 알림
  - 실패하면 결과(응답·로그)를 공유 → Claude가 수정

## 1차 목표 이후 (기록만)
- main 권한·정리 후보 (R-27에서 발견, 범위 밖): `GET /matchings/*/total`이 `lostBoardId` 소유자를 확인하지 않음, 게시글 작성자가 자기 글에 쪽지방을 만들 수 있음, `EmitterService`의 `System.out`·서비스들의 `printStackTrace`, `ReplyMessageReqDto`·`AlarmDataDto`에 기본 생성자 없음(현재 역직렬화는 됨 — 프론트 재구축 때 확인)
- **Naver 로그인 복구 (R-25, D-50)**: 공식 명세 확보 → 위 R-25 항목의 문제 목록 수정 → mock 계약 테스트, 키 미설정 503(D-49), U-06·R-91 1단계. 그 전까지 prod 프로필에는 로그인 수단이 없음
- 이미지: 스토리지 고아 객체 정리(수정·삭제로 떨어진 객체, presign만 받고 안 쓴 객체 — `DeleteObject` 또는 수명주기 규칙), presign 발급자와 등록자 일치 확인 (R-24에서 발견)
- 프론트 재구축 (P3): 기능 유지·디자인 전면 수정, presigned 업로드·Naver(`client_secret` 제외)·FCM·SSE 계약 반영, O-5 결정
- HTTPS 재검토 (O-7), 알림(Alerting)·로그 수집(Loki), Java 21·Boot 4 전환
