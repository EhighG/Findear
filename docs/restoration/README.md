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

1. 깨끗한 clone에서 `.env`만 채우고 루트에서 `docker compose up -d` → **모든 컨테이너가 동시에 떠서 healthy**.
2. 로컬에서 main API 주요 흐름이 동작:
   - 테스트 회원 로그인(로컬 프로필) + Naver 로그인
   - 분실물 등록 → batch → match(mock) 매칭 → (FCM 설정 시) 웹푸시 알림
   - Lost112 습득물 수집(batch, 공공데이터 API) → Elasticsearch → main에서 목록/검색 조회
   - 이미지: presigned URL 발급 → 로컬 S3 대체재(SeaweedFS)에 업로드
   - VWorld 장소 검색 프록시
3. Grafana에서 앱(JVM/HTTP/배치), MySQL, Redis, Elasticsearch, 컨테이너 리소스 지표 확인 가능.
4. 배포 준비물 완비: `compose.prod.yml`, 호스트 초기화·배포 스크립트, GitHub Actions(CI + GHCR 이미지), AWS S3 연동 키트(`infra/aws/`) — AWS 리소스만 만들고 값만 넣으면 전환되는 상태.
5. 레포에 비밀값 없음 (public 레포).

## 4. 문서 목록

| 문서 | 내용 |
|---|---|
| [01-legacy-inventory.md](01-legacy-inventory.md) | 팀 종료 시점(`2af1413`)의 인프라·외부 연동·데이터 저장소 전수 조사, 유실된 정보, 문서↔설정 불일치, 보안 이슈 |
| [02-current-state.md](02-current-state.md) | 현재 `master` 상태: 모듈별 현황, 빌드 확인 결과, 알려진 버그, 브랜치 |
| [03-decisions.md](03-decisions.md) | 요구사항과 결정 로그(D-xx), 미결 사항 |
| [04-target-architecture.md](04-target-architecture.md) | 목표 구성: 서비스, 포트, 볼륨, 리소스(메모리) 산정, 모니터링 설계, 디렉토리 구조 |
| [05-external-integrations.md](05-external-integrations.md) | 외부 연동별 복구 여부·비용·발급 절차·설정값 |
| [06-db-and-config.md](06-db-and-config.md) | DB 스키마(Flyway), ES 인덱스, Redis, 스토리지, 설정 파일 구조, 환경변수 전체 목록 |
| [07-api-contracts.md](07-api-contracts.md) | 서버 간 API 계약(main↔batch↔match), match mock 동작 명세, main 외부 API 목록 |
| [08-work-plan.md](08-work-plan.md) | **작업 분해 + 진행 트래커** (Phase / R-xx / 선행관계 / 완료 기준 / 체크박스), 사용자 작업(U-xx) |
| [09-deploy-and-aws.md](09-deploy-and-aws.md) | 배포 준비(GHCR, compose.prod, 스크립트, Actions), AWS S3 연동 키트 명세 |
| [10-worklog.md](10-worklog.md) | 세션별 작업 로그 |

## 5. 현재 상태 (마지막 갱신: 2026-09-29)

- 조사·설계·문서화 완료. **구현은 아직 시작하지 않음.**
- 다음 작업:
  1. 사용자: U-09(이 문서가 있는 브랜치를 master에 병합), U-01(Naver Secret 재발급), U-02(Secret scanning). 외부 키 발급 U-04~U-07은 승인 대기가 있을 수 있으니 미리.
  2. 코드: R-01(브랜치 삭제) → R-02(레거시 정리) → Phase 1(인프라 골격) → … → R-90(최종 검증 시나리오).
  - 상세는 [08-work-plan.md](08-work-plan.md).

## 6. 세션 인계 규칙

1. 작업 시작 전: 이 README → `08-work-plan.md`(진행 상황) → `10-worklog.md`(최근 로그) 순서로 읽는다.
2. 작업은 `08-work-plan.md`의 R-xx 단위로 한다. 선행 작업이 끝나지 않은 작업은 시작하지 않는다.
3. 새로 결정한 사항은 `03-decisions.md`에 D-번호로 추가한다. P1~P8로 판단되지 않는 사항은 사용자에게 묻는다.
4. 작업 종료 시: `08-work-plan.md` 체크박스·상태 갱신, `10-worklog.md`에 로그 추가, 이 README의 "현재 상태" 갱신.
5. 문서와 실제 코드가 달라지면 **문서를 먼저 고친다** (다음 세션이 문서를 믿고 작업하므로).
6. public 레포다. 비밀값은 절대 커밋하지 않는다 (`.env`, `secrets/`는 git 제외).
