# 10. 작업 로그

> 세션이 끝날 때마다 맨 위에 추가하세요. 형식: 날짜 / 세션(환경·브랜치) / 한 일 / 남은 일·주의사항.

## 2026-09-30 (7) — 로컬 Claude Code, Windows 11 (`feature/15-*`, Phase 2 완료)

**한 일 (Phase 2 마무리, 이슈 #15 / 상위 #12)** — (6)의 중간 인계에서 재개
- R-23 (`feature/15-fcm`): 검증 PASS 뒤 관찰 사항을 보완 — 푸시를 트랜잭션 커밋 후 발송(`@TransactionalEventListener`), 무효 토큰 삭제는 REQUIRES_NEW, `fcm.enabled`는 스프링 boolean 변환(`@ConditionalOnBooleanProperty`는 문자열 비교라 `yes`에서 빈 0개 → 자체 조건). 검증 권고 2건(테스트가 REQUIRES_NEW를 지키게, `PushMessage.toString` 토큰 가림)은 메인이 직접 반영하고 `REQUIRED`로 바꾸면 테스트가 실패하는 것 확인.
- R-24 (`feature/15-image-storage`): presigned PUT 업로드 + object key 저장, V3(`img_key`, `thumbnail_key`). 실행 중 K-14(수정 시 옛 이미지 행이 남음)와 검증 중 K-15(분실물 목록 중복 행 — 쓰지 않는 조인) 발견·수정. K-15 수정이 스크립트 실수로 R-24 커밋에 함께 들어가 커밋 메시지에 명시.
- R-25: 네이버 개발자센터 문서를 WebFetch가 막아 공식 명세를 확인할 수 없음 → 사용자 결정으로 **1차에서 제외, 추후 진행(D-50)**. 확인해 둔 현재 코드 문제는 08 R-25에 기록. 순서를 바꿔 R-26을 먼저 진행.
- R-26 (`feature/15-vworld`): VWorld 공식 문서 대조(파라미터 값은 기존 그대로), 전용 RestTemplate 3s/5s, D-49 공통 예외(503·502).
- R-27 (`feature/15-security-cleanup`): 개발용 기능 local 전용, 공개 경로 단일화, 권한 검사(실행 중 `PATCH /members/{id}/role` 권한 상승 구멍 발견 → 본인만), 오류 응답 규칙(D-51), Redis 키, Testcontainers로 `./gradlew test` 전체 통과.
- 이슈 #15 닫음, 상위 #12의 Phase 2 체크.

**주의**
- prod 프로필은 R-27 이후 로그인 수단이 Naver뿐인데 Naver 로그인은 D-50으로 보류 → 배포 전에 R-25를 해야 함.
- main 테스트는 Docker가 필요함(Testcontainers). Docker 빌드에서 `parent snapshot ... does not exist` 오류가 한 번 났고 재시도로 해결 — 반복되면 `docker builder prune`.
- Windows Git Bash의 curl로 한글을 보내면 CP949로 나가 깨짐 → UTF-8 퍼센트 인코딩 URL이나 UTF-8 파일 본문 사용.

**다음 세션**
- Phase 4(match mock, R-40) → Phase 3(batch, R-30~). batch의 매칭 e2e(R-34·R-35)가 match mock을 쓰므로 Phase 4 먼저. Phase 3 착수 시 batch 엔티티를 V3 컬럼명에 맞출 것(08 R-30 메모). 작업 방식은 D-46 그대로.

## 2026-09-30 (6) — 로컬 Claude Code, Windows 11 (`feature/15-*`, Phase 2 중간)

**한 일 (Phase 2 진행 중, 이슈 #15 / 상위 #12)** — 새 작업 방식(D-46): 지시서 작성 → `findear-executor` 실행 → `findear-verifier` 검증 → 메인이 커밋·master 반영
- R-20 (`feature/15-boot35-build`): Boot 3.5.16, Gradle wrapper 8.14.5, firebase-admin 9.11.0, Querydsl 5.1.0, mail·mariadb·querydsl-sql·httpBasic 제거, Prometheus registry, `main/Dockerfile`(temurin 17 JRE noble, curl 포함, uid 10001). 검증 1회 FAIL(wrapper 스크립트·jar가 8.5 그대로) → 재생성 후 PASS.
- R-11b (`feature/15-main-schema`): Flyway V2(Hibernate 6.6.53 생성 DDL, 이름·순서만 정리, validate 통과 확인), 로컬 전용 반복 시드(D-48), 이미지 컬럼은 R-24의 V3로(D-47). 검증에서 시드 한글 깨짐(mysql 클라이언트 latin1) 발견 → `SET NAMES utf8mb4`.
- R-21 (`feature/15-main-config`): application.yml/-local/-prod, compose `main`, 관리 포트 8081, 모드 B는 루트 `.env` import. 실행 중 **응답이 XML로 나오는 회귀** 발견(firebase-admin → google-cloud-storage → jackson-dataformat-xml + `@EnableWebMvc`) → 기본 콘텐츠 타입 JSON.
- R-22 (`fix/15-main-bugs`): K-01(Lost112 `/search` 경로), K-13(`/losts` sortBy NPE, 이번에 발견), `@Builder.Default` 14건, `PathPatternRequestMatcher`. 검증에서 Ant/PathPattern 매처를 실제 요청 62개로 비교.
- R-23 (`feature/15-fcm`): 실행 완료, **검증 도중 세션 종료(사용량 한도)** → 로컬 브랜치에 커밋만 해 둠(미push). 결과 요약은 08 R-23.
- 발견: K-12(`test-member-type` 헤더만으로 인증되는 개발용 우회 → R-27), 이 PC 호스트 8080은 다른 프로젝트(`simple_board3`)가 사용 → `.env`에 `MAIN_HOST_PORT=8090`(CLAUDE.md 빌드 메모).
- 결정: D-47(이미지 컬럼, 썸네일까지 R-24), D-48(시드 적용 방식), D-49(키 미설정 시 503).
- R-24~R-27 지시서 초안 작성 → 로컬 `.claude/work-orders/`(git 제외). 핵심 결정은 08 각 R-xx 아래에 요약. 이슈 #15 작업 목록은 R-22까지 체크.

**주의**
- `feature/15-fcm`은 R-22 시점 master에서 분기해 이 인계 커밋보다 뒤처져 있음 → master 반영 절차의 `git rebase … master`에서 따라감 (문서만 달라 충돌 없을 것).
- 전체 `./gradlew test`는 DB가 필요한 `MainApplicationTests` 때문에 실패하는 상태 그대로 (R-27에서 Testcontainers로 복구). 단위 테스트는 클래스 지정으로 돌림.
- Git Bash의 curl로 한글 JSON을 보내면 cp949 때문에 main이 `Invalid UTF-8`로 500 → ASCII 본문이나 UTF-8 파일(`--data-binary @file`) 사용.
- 루트 `secrets/`는 compose 디렉토리 마운트 때문에 Docker가 빈 폴더로 만듦 (`.gitignore` 대상).

**다음 세션**
- `git switch feature/15-fcm` → R-23 검증부터. 그다음 R-24 → R-25 → R-26 → R-27, Phase 2가 끝나면 이슈 #15 닫고 보고.
- 사용자: U-10(gh 토큰 2026-10-17 만료) 잊지 말 것.

## 2026-09-30 (5) — 로컬 Claude Code, Windows 11 (`feature/12-subagent-workflow`)

**한 일 (작업 방식 변경, 코드 작업 없음)**
- 사용자 요청으로 역할 분담 도입 (D-46): `.claude/agents/findear-executor.md`(Sonnet 5.5 high, 지시서대로 실행·보고, git·진행 문서는 손대지 않음), `findear-verifier.md`(Opus 5.5 high, 완료 기준 재확인·범위·규칙 검사, 레포 읽기 전용). CLAUDE.md에 "작업 방식"(R-xx 흐름, 작업 지시서에 담을 것), 08 진행 절차·03·04 §7·README 반영.
- 메인 세션 기본값은 사용자 전역 설정(`~/.claude/settings.json`)에 이미 `model: opus` + Opus 5.5 effort `xhigh`로 있어서 따로 바꾸지 않음. max는 설정 파일에 저장되지 않음 → 필요하면 세션마다 `/effort max`. 개인 설정 파일이 실수로 커밋되지 않게 `.gitignore`에 `.claude/settings.local.json` 추가.
- 확인한 문서: Claude Code 공식 문서 "Subagents"(frontmatter `model`·`effort`·`tools`, subagent도 CLAUDE.md를 읽음, 새 `agents` 폴더는 세션 재시작 후 인식), "Settings"(`effortLevel` 값 low~xhigh, `model`).

**주의**
- `.claude/agents/`는 이번에 처음 만든 폴더라 **Claude Code를 재시작해야** subagent가 인식된다 (`/clear`로는 안 됨).

**다음 세션**
- Claude Code 재시작 후 Phase 2 (R-20 → R-11b → R-21 → … → R-27)를 새 작업 방식으로.

## 2026-09-30 (4) — 로컬 Claude Code, Windows 11 (`feature/14-*`, Phase 1)

**한 일 (Phase 1 완료, 이슈 #14 / 상위 #12)** — R-xx마다 브랜치 → master 반영
- R-10 (`feature/14-compose-infra`): `compose.yml`(MySQL 8.4.11, Redis 8.8.3, ES 8.19.22, SeaweedFS 4.48, 헬스체크·명명 볼륨·네트워크 `findear`·최소 사양 메모리 제한), `compose.override.yml`(127.0.0.1 게시, `*_HOST_PORT`), `.env.example`(쓰는 변수만, D-43). 재기동 후 MySQL·ES 데이터 유지 확인.
- R-11a (`feature/14-flyway`): `flyway` one-shot + `V1__spring_batch_schema.sql`(spring-batch-core 5.2.6 = Boot 3.5.16 관리 버전). 재실행 시 추가 적용 없음.
- R-12 (`feature/14-mysql-initdb`): `infra/mysql/initdb/01-exporter-user.sh` (exporter 계정, 읽기 전용 권한 확인).
- R-13 (`feature/14-seaweedfs`): `infra/seaweedfs/`(s3.json 템플릿·entrypoint·storage-init). 검증 11항목 통과 (서명 업로드, 잘못된 키 거부, `images/*`만 익명 GET, CORS, presigned GET, 재기동 후 유지).
- R-14 (`feature/14-monitoring-infra`): Prometheus·Grafana(데이터소스 provisioning)·cAdvisor·exporter 3종 (profile `monitoring`). 타깃 6개 UP, Grafana 관리자 로그인·데이터소스 질의 확인.
- 결정: D-44(호스트 포트 변수화), D-45(SeaweedFS 공개 읽기를 AWS와 같은 버킷 정책으로). 계획 리뷰 결정 D-40~D-43은 (3) 참고.
- 모든 검증은 D-32대로 필요한 서비스만 부분 기동 후 `down -v`. 외부 API·AWS 호출 없음 (aws-cli·curl은 로컬 SeaweedFS만).

**주의**
- 이 PC는 Windows용 MySQL 8.0 서비스(`MySQL80`)가 3306을 씀 → `.env`에 `MYSQL_HOST_PORT=3307`. 다른 프로젝트 컨테이너(`simple_board3`, `momap`)도 떠 있음, 건드리지 않음.
- Git Bash에서 docker 명령에 컨테이너 경로를 넘길 때 `MSYS_NO_PATHCONV=1` 필요.
- Docker Desktop의 bind mount 파일은 실행 가능으로 보임 → MySQL initdb 스크립트가 source가 아니라 실행됨 (스크립트를 그에 맞게 작성, 100755로 고정).
- Flyway OSS 이미지의 드라이버는 MariaDB Connector/J 2.7 → `allowPublicKeyRetrieval=true` 필요 ([06 §1](06-db-and-config.md#1-mysql)). main·batch(MySQL Connector/J)는 기본 SSL로 통과하지만 SSL을 끄면 같은 옵션 필요.
- SeaweedFS 로그의 `no signing key found for STS service` 오류는 쓰지 않는 STS 기능 로그 (인증은 정상).
- cAdvisor는 Docker Desktop에서 동작하지만 컨테이너별 파일시스템 사용량은 없음 ([04 §6](04-target-architecture.md#접근보안)).
- aws-cli 종료 코드: 서비스 오류는 1이 아니라 254.

**다음 세션**
- Phase 2 (R-20 → R-11b → R-21 → … → R-27) — Phase 4(match mock)와 병렬 가능. 착수 시 이슈를 `gh issue create --parent 12`로 생성. main은 compose에 `main` 서비스(빌드·8080/8081·depends_on flyway 등)를 추가해야 함 (R-20 Dockerfile 이후).

## 2026-09-30 (3) — 로컬 Claude Code, Windows 11 (`feature/12-plan-review-phase1`)

**한 일 (Phase 1 착수 전 계획 리뷰, 코드 작업 없음)**
- 사용자가 Phase 1 계획을 리뷰하면서 Claude가 짚은 문제 두 가지를 결정: R-11은 Phase 2(R-20·R-21)에 걸려 있어 Phase 1 안에서 끝낼 수 없음 → **분할**(D-40), aws-cli는 presigned PUT URL을 만들 수 없음 → 사용자 지시로 **AWS 실제 연결이 필요한 검증은 전부 생략**(D-41), 나머지는 Claude가 결정(D-42 R-13 검증 방법, D-43 `.env.example` 범위).
- 반영: 08(R-10 영속성 기준에서 Redis 제외, R-11a·R-11b, R-13 완료 기준, R-21 validate 기동, R-24, R-62~R-65 검증 범위, R-90 `.env.example` 대조, U-08), 06 §2(마이그레이션 번호 V1=batch 메타, V2=main), §6, §8, README(DoD 3·5번, 현재 상태), 05(원칙 5번, §6, §8에 AWS CLI 문서 확인 기록), 09 §3·§4, CLAUDE.md.

**다음 세션**
- Phase 1 (R-10 → R-11a → R-12 → R-13 → R-14). 착수 시 Phase 1 이슈를 `gh issue create --parent 12`로 생성.

## 2026-09-30 (2) — 로컬 Claude Code, Windows 11 (`master`, `feature/13-legacy-cleanup`)

**한 일 (Phase 0 완료, 이슈 #13 / 상위 #12)**
- R-00: 상위 이슈 #12 "Findear 복구 1차", Phase 0 이슈 #13(#12의 sub-issue) 생성. gh 안전장치 확인. 문서 브랜치 커밋 5개(계획엔 3개로 적혀 있었음)에 `Related to #12`를 트레일러 앞에 붙여 master에 fast-forward 병합·push(`d4f6025..bf51b1b`), 원격·로컬 `claude/happy-babbage-qt991n` 삭제.
- R-01: 원격 `Chore/10-reset_env`(`76edc42`) 삭제. `old-master`는 그대로.
- R-02 (`feature/13-legacy-cleanup`): 레거시 삭제(D-22 목록 + `infra/README.md`), 팀 batch → `batch/`, `exec/` → `docs/legacy/`·`infra/db/dummy/`, 루트 `.gitignore`·`.gitattributes`, `gradlew` +x(작업 트리도 LF로 다시 받음), K-08, `batch/.gitignore`의 `*.yml` 규칙 제거, 이슈 참조 rebase 보조 스크립트 `tools/git/add-issue-ref.sh`(D-39). 계획 외로 한 것은 [08 R-02](08-work-plan.md#phase-0--정리)에 정리.
- 검증: 비밀값 정규식(08 상단·R-02) 모두 0건, 추적 중인 `.env`는 `front/.env`(URL만, 키 값 없음)뿐, main `compileJava`·`compileTestJava` 성공 + `LostBoardQueryServiceTest` 4/4 (JDK 21, Git Bash에서 `./gradlew`).
- 문서: 08(절차에 스크립트·#12, 비밀값 검사 예외, U-10, R-80 python 메모), 03(D-39), README 현재 상태, 02(Phase 0 이후 경로 안내), 06 §8(더미 위치·주의), 04 §7(`tools/git/`), 01 §10, CLAUDE.md.

**주의**
- gh의 fine-grained PAT에 Issues 쓰기 권한이 없어 이슈 생성이 한 번 막혔고(`Resource not accessible by personal access token (createIssue)`), 사용자가 Issues: Read and write를 추가함. **토큰 만료 2026-10-17** (응답 헤더로 확인) → U-10.
- git push는 gh 토큰이 아니라 Git Credential Manager 자격증명을 씀 (`credential.helper=manager`).
- 이 PC에서 `python3`는 Microsoft Store 별칭이라 실행되지 않음(exit 49) → `python`(3.14) 사용. rebase exec 스크립트도 이 문제로 한 번 실패해서 `tools/git/add-issue-ref.sh`는 sh+awk로 작성.
- `infra/db/dummy/dummyScript_Agency.sql`은 `tbl_Agency`(대문자)라 Linux MySQL에서 실패 → [06 §8](06-db-and-config.md#8-시드더미-데이터)에 기록. 파일명(`dummyScript_,Member.sql` 포함)은 그대로 둠.

**다음 세션**
- Phase 1 (R-10 → R-11 → R-12 → R-13 → R-14). 착수 시 Phase 1 이슈를 `gh issue create --parent 12`로 생성. Phase 1이 끝나면 보고.

## 2026-09-30 — 로컬 Claude Code, Windows 11 (`claude/happy-babbage-qt991n`)

**한 일 (사용자 피드백을 계획에 반영, 코드 작업 없음)**
- D-38 추가: 외부 API(Naver, VWorld, FCM, Lost112, AWS)는 작업·검증 중 호출하지 않음. 공식 문서 기준 구현 + mock 계약 테스트로 "키만 넣으면 동작"하게 완성, 실제 확인은 키 세팅 후 사용자(R-91).
- [08](08-work-plan.md): U-04·U-06·U-07도 1차 작업 후로, R-21·R-23·R-25·R-26·R-32·R-64·R-80 완료 기준을 mock 검증으로 변경, R-81(키 세팅 체크리스트·`tools/verify-external/`) 신설, R-90에서 외부 연동 단계 제거(미설정 상태 점검으로 대체), R-91(사용자 확인) 신설.
- [05](05-external-integrations.md): 연동별 "1차 작업 중 검증 / 키 세팅 후 확인" 분리, §8 공식 문서 확인 기록, §9 키 세팅 체크리스트 추가.
- 옛 이슈 #1~#11이 모두 닫힌 것 확인 (`gh issue list`) → R-00에서 #11 닫기 단계 삭제, D-33 수정.

**다음 세션**
- R-00 → R-01 → R-02 → Phase 1. Phase 0이 끝나면 보고.

## 2026-09-29 (2) — 로컬 Claude Code, Windows 11 (`claude/happy-babbage-qt991n`)

**한 일**
- 진행 방식 확정, 결정 D-31~D-37 기록 ([03](03-decisions.md)): 메모리 기본값 최소 사양, 전체 동시 기동·자원 실측 안 함, Phase별 이슈 + R-xx별 브랜치, master 반영은 Claude가 하고 Phase마다 보고, 세션은 Phase 단위, 원본 레포 쓰기 금지, U-01·U-02·U-05는 1차 작업 후.
- [04 §5](04-target-architecture.md#5-리소스-산정-메모리) 자원 표를 최소 사양 기준으로 다시 산정 (합계 약 3.6GB, 실측 아님). JVM `MaxRAMPercentage` 70 → 50, ES 힙 768m → 512m.
- (2026-09-30 사용자 피드백 반영) 튜닝은 일반적인 사용 방식 안에서만: 처음 넣었던 SerialGC 지정, ES ML·GeoIP 끄기, `GOMEMLIMIT`, cAdvisor `--disable_metrics`를 뺌. 최종 검증 R-90은 모니터링까지 전체를 띄우고 `docker stats`로 실측하도록 되돌림 (개발 중에는 부분 기동만).
- [08](08-work-plan.md): 상단에 진행 절차·비밀값 검사 추가, R-00(작업 준비) 신설, U-09는 R-00으로 흡수, R-10·R-14·R-32·R-51·R-90 완료 기준을 부분 기동 기준으로 수정 (R-90은 묶음 A·B·C).
- 원본 레포 보호 (D-36): `.claude/settings.json` 추가(`GH_REPO=EhighG/Findear`, `2TF4`/`2tf4` 포함 Bash·PowerShell 명령 deny), 로컬 `gh repo set-default EhighG/Findear`. 이 세션에서 둘 다 적용되는 것 확인 (`echo …2TF4` 차단됨, `gh repo view` → `EhighG/Findear`).
- 확인한 사실: 사용자 계정은 `2TF4/findear`의 admin (`gh api repos/2TF4/findear`의 permissions). gh 토큰은 fine-grained PAT(만료 2026-10-17)이고 Actions 설정 조회는 403.
- 로컬 환경: Docker 29.3.1 / Compose v5.1.0, Docker VM 메모리 16.5GB·8코어, JDK 21, gh 2.101.0, `core.autocrlf=true`.

**하지 않은 것**
- 자원 실측은 사용자 요청으로 중단. 그 전에 받아진 이미지 7개(`seaweedfs:4.48`, `redis:8.8.3`, `prometheus:v3.15.0`, `cadvisor:v0.55.1`, `mysqld-exporter:v0.20.0`, `redis_exporter:v1.92.1`, `elasticsearch-exporter:v1.11.0`)는 로컬에 남아 있음 (R-10·R-14에서 재사용). 컨테이너는 띄우지 않음.
- 이슈 생성·#11 닫기·master 병합(R-00)은 아직 안 함.

**다음 세션**
- R-00 → R-01 → R-02 → Phase 1. Phase 0이 끝나면 보고.

## 2026-09-29 — Claude Code on the web (`claude/happy-babbage-qt991n`)

**한 일**
- 전체 git 히스토리(845 커밋, 18 브랜치) 조사: 팀 종료 시점 인프라 인벤토리([01](01-legacy-inventory.md)), 유실 정보·보안 이슈 정리.
- 사용자와 복구 방향 합의: 요구사항 P1~P8, 결정 D-01~D-30 ([03](03-decisions.md)).
- 현재 master 상태 점검([02](02-current-state.md)): main은 JDK 21 + Gradle 8.5로 컴파일 성공, `LostBoardQueryServiceTest` 4/4 통과. 알려진 문제 K-01~K-11 기록.
- 목표 구성·리소스 산정·모니터링 설계([04](04-target-architecture.md)), 외부 연동([05](05-external-integrations.md)), DB·설정·환경변수([06](06-db-and-config.md)), API 계약·match mock 명세([07](07-api-contracts.md)), 작업 계획([08](08-work-plan.md)), 배포·AWS 키트 명세([09](09-deploy-and-aws.md)) 작성. 루트 `CLAUDE.md` 추가.
- 이미지 버전은 2026-09-29 Docker Hub 기준으로 확인 (MinIO 공식 이미지가 Docker Hub에서 사라진 것 확인 → SeaweedFS 선택).

**하지 않은 것**
- 코드·인프라 구현은 시작하지 않음. `Chore/10-reset_env` 브랜치 삭제(R-01)도 아직 안 함.

**다음 세션**
1. 사용자: U-01(Naver Secret 재발급), U-02, U-09(이 브랜치 master 병합), U-04~U-07 발급 미리 진행.
2. R-01 → R-02 → Phase 1부터 [08](08-work-plan.md) 순서대로.

**추가 (같은 날)**
- 1차 목표 최종 검증 시나리오 R-90을 [08](08-work-plan.md)에 추가, README의 다음 작업 순서 정리.

**주의**
- 클라우드 세션에서 Maven Central이 간헐적으로 429를 반환함 → 잠시 후 `--max-workers=1`로 재시도.
- `gradlew` 실행 권한이 없어 `sh ./gradlew`로 실행 (R-02에서 수정 예정).
