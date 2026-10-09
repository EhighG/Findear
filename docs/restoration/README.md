# Findear 복구 작업 (2026 Restoration)

> 이 폴더는 Findear 복구 작업의 **단일 기준 문서**입니다.
> 다른 세션(Claude Code web / 로컬 Claude Code / 사람)이 이어서 작업할 수 있도록, 조사 결과·결정·작업 계획·진행 상황을 모두 여기에 기록합니다.
> 작업을 시작하기 전에 이 파일과 [08-work-plan.md](08-work-plan.md)를 먼저 읽으세요.

## 1. 배경

| 시점 | 기준 | 설명 |
|---|---|---|
| 팀 프로젝트 종료 | `2af1413` (2024-05-23), 브랜치 `old-master` | SSAFY 팀 프로젝트. 모든 인프라가 활성화돼 있던 상태 (EC2 + Docker Compose + Jenkins + Config Server + ES + Redis + FCM + Lost112 API + OpenAI 등) |
| 개인 리팩토링 | `master` `d4f6025` (코드 작업 2024-08~12) | 인프라를 대폭 생략하고 main 위주로 리팩토링. batch/match는 stub, FCM 비활성 |
| 이번 복구 | 2026-09-29 ~ | 두 시점 어느 쪽도 아닌 **재구성**. 필요한 인프라만 복구 + 모니터링 추가, 로컬 실행 / 배포 준비 상태 |

## 2. 이번 복구의 요구사항 (사용자 결정, 원칙)

| # | 요구사항 |
|---|---|
| P1 | match 서버의 AI 기능(OpenAI, fastText 등)은 복구하지 않는다. **main, batch가 주 복구 대상.** match와 다른 서버 간 상호작용은 유지하고, match API는 임의 로직으로 동작하는 mock으로 추상화한다. |
| P2 | 인프라는 필요한 만큼만 복구한다. 기존과 똑같을 필요 없다 (Jenkins, Spring Cloud Config 등은 필요 여부 판단). |
| P3 | 프론트엔드는 1차 복구 목표에 포함하지 않는다. 나중에 복구할 땐 기능은 유지, 디자인은 전면 수정. |
| P4 | 기능상 필요한 외부 연동 중 **무료인 것은 전부 복구**. 유료는 사용자에게 보고 후 사용자가 판단. |
| P5 | 외부 배포는 언제든 가능하도록 스크립트·로직·기술스택만 갖춰둔다 (EC2 등만 확보하면 바로 연동되는 상태). **실제 실행은 로컬.** |
| P6 | HTTPS는 하지 않는다. |
| P7 | 언급되지 않은 사항은 사용자에게 보고하거나, P1~P6에 따라 명확하면 그 방향으로 결정한다. |
| P8 | 모니터링 추가: **Prometheus + Grafana** 기반으로 각 구성요소의 성능 상태를 파악할 수 있어야 한다. (2026-09-29 추가) |

세부 결정 사항은 [03-decisions.md](03-decisions.md)에 D-번호로 기록되어 있습니다.

## 3. 1차 목표 완료 기준 (Definition of Done)

1. 깨끗한 clone에서 `.env`만 채우고 루트에서 `docker compose up -d` → **모니터링까지 모든 컨테이너가 동시에 떠서 healthy** (헬스체크를 둘 수 없는 redis-exporter는 running + Prometheus `up`=1, one-shot인 flyway·storage-init은 exit 0). 메모리 제한 기본값은 최소 사양(D-31). 전체 동시 기동과 자원 실측은 개발 중에는 하지 않고 최종 검증(R-90)에서 한다 (D-32).
2. 로컬에서 main API 주요 흐름이 동작 (외부 키 없이):
   - 테스트 회원 로그인(로컬 프로필)
   - 분실물 등록 → batch → match(mock) 매칭 (FCM은 비활성 상태에서 오류 없이 건너뜀)
   - Lost112 샘플 문서 → Elasticsearch → main에서 목록/검색 조회
   - 이미지: presigned URL 발급 → 로컬 S3 대체재(SeaweedFS)에 업로드
   - 쪽지 전송·답장
3. **외부 연동(VWorld, Lost112 수집, FCM 웹푸시, AWS S3 키트)은 공식 문서 기준으로 구현되어 mock 계약 테스트를 통과하고, 사용자가 키만 세팅하면 코드 수정 없이 동작하는 상태** (D-38). 작업·검증 중 외부 API는 호출하지 않으며, 실제 동작 확인은 1차 작업 후 사용자가 R-91에서 한다. AWS S3 키트는 mock 대신 로컬 SeaweedFS로 같은 명령을 확인하고, AWS 연결이 필요한 검증은 하지 않는다 (D-41). Naver 로그인은 1차 범위에서 제외하고 추후 진행한다 (D-50).
4. Grafana에서 앱(JVM/HTTP/배치), MySQL, Redis, Elasticsearch, 컨테이너 리소스 지표 확인 가능.
5. 배포 준비물 완비: `compose.prod.yml`, 호스트 초기화·배포 스크립트, GitHub Actions(CI + GHCR 이미지), AWS S3 연동 키트(`infra/aws/`) — AWS 리소스만 만들고 값만 넣으면 전환되는 상태. 검증은 로컬에서 가능한 범위(`compose.prod.yml` config, 스크립트·JSON 문법, SeaweedFS)까지이고, AWS·EC2 연결 확인은 배포할 때 사용자가 한다 (D-41).
6. 레포에 비밀값 없음 (public 레포).

## 4. 문서 목록

| 문서 | 내용 |
|---|---|
| [01-legacy-inventory.md](01-legacy-inventory.md) | 팀 종료 시점(`2af1413`)의 인프라·외부 연동·데이터 저장소 전수 조사, 유실된 정보, 문서↔설정 불일치, 보안 이슈 |
| [02-current-state.md](02-current-state.md) | 현재 `master` 상태: 모듈별 현황, 빌드 확인 결과, 알려진 버그, 브랜치 |
| [03-decisions.md](03-decisions.md) | 요구사항과 결정 로그(D-xx), 미결 사항 |
| [04-target-architecture.md](04-target-architecture.md) | 목표 구성: 서비스, 포트, 볼륨, 리소스(메모리) 산정, 모니터링 설계, 디렉토리 구조 |
| [05-external-integrations.md](05-external-integrations.md) | 외부 연동별 복구 여부·비용·발급 절차·설정값, 공식 문서 확인 기록(§8), **키 세팅 체크리스트(§9)** |
| [06-db-and-config.md](06-db-and-config.md) | DB 스키마(Flyway), ES 인덱스, Redis, 스토리지, 설정 파일 구조, 환경변수 전체 목록 |
| [07-api-contracts.md](07-api-contracts.md) | 서버 간 API 계약(main↔batch↔match), match mock 동작 명세, main 외부 API 목록 |
| [08-work-plan.md](08-work-plan.md) | **작업 분해 + 진행 트래커** (Phase / R-xx / 선행관계 / 완료 기준 / 체크박스), 사용자 작업(U-xx) |
| [09-deploy-and-aws.md](09-deploy-and-aws.md) | 배포 준비(GHCR, compose.prod, 스크립트, Actions), AWS S3 연동 키트 명세 |
| [10-worklog.md](10-worklog.md) | 세션별 작업 로그 |

## 5. 현재 상태 (마지막 갱신: 2026-10-09, R-91 외부 연동 확인 완료)

- **R-91(키 세팅 후 외부 연동 확인) 완료** (2026-10-09, 상위 이슈 #12): 사용자가 U-01·U-04·U-05·U-07을 끝내고 키를 채움, 확인 요청은 사용자 요청으로 Claude가 실행(D-63). VWorld 검색·주소 변환 OK, Lost112 실제 수집 성공(경찰청 50,742건·포털기관 42,052건 → ES 89,528건, main 목록 조회 200), FCM 브라우저 알림 수신(사용자)·`verify.sh --fcm-phone` 200. **DoD 3의 "키만 넣으면 코드 수정 없이 동작"을 실제로 확인** — 앱 코드는 고치지 않았고, `verify.sh`의 테스트 발송 본문만 Windows Git Bash 한글 인자 문제로 stdin 전송으로 수정. Lost112 트래픽 한도는 건수가 아니라 호출 수(기본값으로 1회 수집 약 94회). 상세는 08 R-91 결과.

- **Phase 8(1차 목표 최종 검증) 완료** (2026-10-01, 이슈 #21): master `897a86f`의 깨끗한 clone에서 `.env`의 호스트 포트만 바꿔 모니터링까지 15개 서비스를 한 번에 기동(약 73초, 빌드 캐시 사용) → 08 R-90 시나리오 1~11 통과(로그인·이미지·습득물 자동채움·분실물 매칭·Lost112 샘플·스케줄 잡·쪽지·외부 연동 미설정 점검·Prometheus 9/9 up·패널 error 0). **DoD 1·2·4·6 확인, 3·5는 Phase 6·7 산출물과 이번 점검으로 충족** — 외부 연동 실제 동작(R-91)·AWS·EC2 연결(U-08)은 사용자 몫. 자원: OOM·재시작 0, 사용 합계 약 3,410MiB/제한 4,000MiB → 04 §5 "R-90 실측" 열. 결정 D-62(기본값은 최소 그대로, ES·MySQL·cAdvisor·Prometheus·main은 오래 켜 두는 환경·배포에서 "여유" 값 권장). 예전 메모의 "등록 API 2.1초"는 서버가 아니라 Windows 클라이언트의 `localhost`(`::1` 먼저 시도) 지연으로 확인.

- **Phase 7(검증 도구) 완료** (2026-10-01, 이슈 #20): FCM 웹푸시 테스트 페이지 `tools/fcm-test/`(R-80 — Firebase JS SDK 12.19.0, `firebase-config.js`가 없거나 비어 있으면 SDK를 불러오지 않고 설정 안내만, 상태 `body[data-fcm-state]`), 외부 연동 확인 스크립트 `tools/verify-external/verify.sh`와 05 §9 키 세팅 체크리스트(R-81 — 설정 검사 → 설정된 연동만 main·batch로 확인 → 요약, 키가 없으면 HTTP 요청 0). 결정 D-61: 웹·Admin SDK 문서가 등록 토큰을 deprecated로 두고 FID를 권장하지만 1차는 등록 토큰 유지, FID 전환은 1차 이후. 외부 API는 호출하지 않았고 실제 토큰 발급·알림 수신·연동 확인은 R-91.

- **Phase 0(정리) 완료** (2026-09-30, 이슈 #13, 상위 이슈 #12): 복구 문서 master 반영, `Chore/10-reset_env`·문서 브랜치 삭제, 레거시 삭제·이동(D-22), 루트 `.gitignore`·`.gitattributes`, `gradlew` 실행 권한, K-08. 트리는 [04 §7](04-target-architecture.md#7-목표-디렉토리-구조) 기준(아직 만들지 않은 폴더 제외). **구현은 Phase 1부터.**
- **Phase 1(인프라 골격) 완료** (2026-09-30, 이슈 #14): `compose.yml`·`compose.override.yml`·`.env.example`로 MySQL·Redis·ES·SeaweedFS, Flyway(V1 Spring Batch 메타), MySQL exporter 계정, SeaweedFS 자격증명·storage-init(버킷·CORS·`images/*` 공개 정책), 모니터링(Prometheus·Grafana·cAdvisor·exporter 3종, profile `monitoring`)까지 구성하고 부분 기동으로 검증. 결정 D-44(호스트 포트 변수화), D-45(공개 읽기는 버킷 정책). 앱(main·batch·match)은 아직 compose에 없음.
- **Phase 2(main 복구) 완료** (2026-09-30, 이슈 #15): main을 Boot 3.5.16으로 올리고(R-20) Flyway V2·V3 스키마와 로컬 시드(R-11b, D-47·D-48), 설정 외부화·compose `main`(R-21), K-01·K-13 등 버그(R-22), FCM 조건부 초기화·커밋 후 발송(R-23), presigned PUT 업로드 + key 저장(R-24, K-14·K-15), VWorld 공식 명세 대조·키 미설정 503(R-26, D-49), 개발용 기능 local 전용·권한 검사·오류 응답 규칙·Testcontainers(R-27, D-51). **R-25(Naver 로그인)는 공식 문서를 열람할 수 없어 1차에서 제외, 추후 진행(D-50)** — 그 전까지 prod 프로필에는 로그인 수단이 없음. main `./gradlew test` 전체 통과(Docker 필요).
- **Phase 5(모니터링 연결) 완료** (2026-10-01, 이슈 #18): Prometheus가 앱 3개(main·batch·match)를 수집하고, main HTTP 클라이언트 시간 제한(상대가 멈췄을 때 실패를 약 3초에 앎)·커스텀 지표 3종(Lost112 수집 실행·건수, FCM 발송 결과)·Spring Batch 지표 중복 WARN 제거(R-50), Grafana에 가져온 대시보드 6개(JVM·Spring Boot·MySQL·Redis·ES·cAdvisor)와 "Findear Overview"를 provisioning(R-51). Grafana 메모리 기본값 512MB(D-57). 전체 타깃 동시 확인과 메모리 실측은 R-90.
- **Phase 6(배포 준비) 완료** (2026-10-01, 이슈 #19): CI `ci.yml`(push·PR마다 main·batch·match 빌드·테스트, Gradle 캐시 basic — D-59, R-60), GHCR 이미지 `images.yml`(수동 실행·master만, 태그 `latest`·전체 SHA, 로컬 override는 `pull_policy: build` — D-58, R-61), 배포 오버레이 `compose.prod.yml`(prod 프로필 고정 + main·batch의 local+prod 가드 D-60, 스토리지는 AWS S3만, main만 80, Redis·ES 인증, 모니터링 127.0.0.1, node-exporter·Node Exporter Full, restart·로그 로테이션 — prod 오버레이를 로컬에서 부분 기동해 인증·프로필 확인, R-62), `infra/deploy/init-host.sh`·`deploy.sh`(`.env` 검사·`--check`·`--tag` 롤백, R-63), AWS S3 키트 `infra/aws/`(SeaweedFS 리허설, R-64), 수동 SSH 배포 `deploy.yml`(R-65). **실제 배포·AWS·EC2 연결 확인은 하지 않음(D-41)** — node-exporter(Docker Desktop에서 기동 불가), IMDS hop limit 등 서버에서 확인할 것은 08 R-62~R-65 메모.
- **Phase 3(batch 복구) 완료** (2026-10-01, 이슈 #17): 팀 batch를 Boot 3.5.16으로(R-30, jakarta·Batch 5·`ElasticsearchOperations`·Spring Cloud 제거) 올리고 설정 외부화·compose `batch`(R-31), Lost112 수집을 공식 명세(공공데이터포털 상세 페이지, 05 §8) 기준 페이지 단위 bulk upsert로 재작성 + 샘플 적재 `infra/elasticsearch/seed/`(R-32, D-53), 매칭 로그 매핑·결정적 ID·분실물 단위 교체(R-33, D-54), findearJob·policeJob 복원과 분실물 단위 실패 격리·수동 실행 API(R-34, D-55), main 분실물 등록 직후 매칭을 커밋 후 요청으로·Lost112 쿼리 인코딩 + main·batch·match 전체 흐름 e2e(R-35), 개발용 엔드포인트 local 한정·오류 응답 규칙(R-36, D-56). batch 테스트 143개, main 235개(둘 다 Testcontainers, Docker 필요). Lost112 실제 응답 구조·트래픽은 R-91에서 확인.
- **Phase 4(match mock) 완료** (2026-09-30, 이슈 #16): `match/` Spring Boot 3.5 mock 앱(R-40) — 팀 시절 경로·JSON 그대로 `/process`(결정적 카테고리·색상·키워드 5개), `/matching/findear`·`/matching/lost`(교체 가능한 `MatchingScorer` + 결정적 기본 점수), 관리 포트 8085, compose `match`. R-40 착수 때 main 습득물 자동채움이 트랜잭션 커밋 전에 match를 호출하고 등록 시점 엔티티를 merge하는 문제를 발견해 R-41로 수정 — 커밋 후 요청, 빈 컬럼만 채움(D-52). batch와의 실제 연동은 Phase 3(R-34·R-35)에서 확인함.
- **작업 방식 변경 (2026-09-30, D-46)**: Phase 2부터 메인 세션은 작업 지시서 작성·git·문서를 맡고, 실행은 `findear-executor`(Sonnet 5.5 high), 검증은 `findear-verifier`(Opus 5.5 high) subagent가 한다 (`.claude/agents/`, CLAUDE.md "작업 방식").
- Phase 1 착수 전 사용자 계획 리뷰 반영 (2026-09-30): R-11을 R-11a(Phase 1: Flyway + Spring Batch 메타 스키마 V1)와 R-11b(Phase 2: main 스키마 V2·시드)로 분할(D-40), AWS 실제 연결이 필요한 검증은 생략(D-41), R-13은 presigned GET까지 확인하고 presigned PUT은 R-24에서(D-42), `.env.example`은 R-xx별로 추가(D-43).
- 외부 API는 작업·검증 중 호출하지 않고, 공식 문서 기준 구현 + mock 계약 테스트로 "키만 넣으면 동작"하게 완성 (D-38). 이슈는 상위 이슈 #12 아래 Phase별 sub-issue로 운영 (D-33, 옛 이슈 #1~#11은 사용자가 닫음).
- 진행 방식 확정 (D-31~D-37, D-39): 메모리 기본값 최소 사양(튜닝은 일반적인 방식 안에서만), 개발 중에는 부분 기동만 하고 전체 기동·실측은 R-90에서, Phase별 이슈 + R-xx별 브랜치, master 반영은 Claude가 하고(이슈 참조는 `tools/git/add-issue-ref.sh`) Phase마다 보고, 세션은 Phase 단위, 원본 레포(`2TF4/findear`) 쓰기 금지.
- 원본 레포 보호 장치 적용됨: `.claude/settings.json`(GH_REPO 고정 + `2TF4` 포함 명령 차단), 로컬 `gh repo set-default EhighG/Findear`.
- 다음 작업:
  1. 코드: Claude의 1차 작업과 R-91 확인까지 끝. 이후는 사용자 결정에 따라 — "1차 목표 이후"(08: FID 전환 D-61, Naver 로그인 R-25, 프론트 재구축 등). push마다 CI가 돈다.
  2. 사용자: **U-10**(gh 토큰이 2026-10-17 만료 → 그 전에 같은 권한(Issues R/W, Actions R/W)으로 갱신). U-02 중 Push protection 켜졌는지 확인(Settings → Security and quality → Advanced Security → Secret Protection → Push protection). Lost112 정기 수집을 켤지(`LOST112_COLLECT_ENABLED`). 배포를 결정하면 U-08(08의 U-08 순서, 04 §5의 "여유" 값 권장).
  - 상세는 [08-work-plan.md](08-work-plan.md).

## 6. 세션 인계 규칙

1. 작업 시작 전: 이 README → `08-work-plan.md`(진행 상황) → `10-worklog.md`(최근 로그) 순서로 읽는다.
2. 작업은 `08-work-plan.md`의 R-xx 단위로 한다. 선행 작업이 끝나지 않은 작업은 시작하지 않는다.
3. 새로 결정한 사항은 `03-decisions.md`에 D-번호로 추가한다. P1~P8로 판단되지 않는 사항은 사용자에게 묻는다.
4. 작업 종료 시: `08-work-plan.md` 체크박스·상태 갱신, `10-worklog.md`에 로그 추가, 이 README의 "현재 상태" 갱신.
5. 문서와 실제 코드가 달라지면 **문서를 먼저 고친다** (다음 세션이 문서를 믿고 작업하므로).
6. public 레포다. 비밀값은 절대 커밋하지 않는다 (`.env`, `secrets/`는 git 제외). push 전 [08](08-work-plan.md)의 "비밀값 검사"를 수행한다.
7. 이슈·브랜치·master 반영 절차는 [08](08-work-plan.md) 상단 "진행 절차"를 따른다. 원본 레포 `2TF4/findear`에는 어떤 쓰기도 하지 않는다 (D-36).
