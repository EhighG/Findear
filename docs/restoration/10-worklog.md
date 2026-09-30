# 10. 작업 로그

> 세션이 끝날 때마다 맨 위에 추가하세요. 형식: 날짜 / 세션(환경·브랜치) / 한 일 / 남은 일·주의사항.

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
