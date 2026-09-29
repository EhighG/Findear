# 03. 결정 로그

> 요구사항(원칙) P1~P8은 [README](README.md#2-이번-복구의-요구사항-사용자-결정-원칙)에 있습니다.
> 새 결정은 아래 표에 D-번호를 이어서 추가하세요. "결정 주체"가 Claude인 항목은 원칙(P1~P8)에 따라 정한 것이며, 사용자가 이견을 내면 바꿀 수 있습니다.

## 결정 사항

| ID | 결정 | 근거 | 결정 주체 | 일자 |
|---|---|---|---|---|
| D-01 | 주 복구 대상은 **main, batch**. match는 AI 없이 **mock**으로 대체하되 서버 간 상호작용은 유지. 프론트는 1차 제외 | 사용자 요구 | 사용자 | 2026-09-29 |
| D-02 | 코드 기준점: main = `master`의 main, batch = 팀 batch(`old-servers/batch`)를 복원 후 업그레이드, stub batch 삭제, `Chore/10-reset_env`는 참고하지 않음 | master main은 리팩토링 반영본, stub은 기능 부족, 팀 batch가 실제 기능 보유 | Claude 제안 → 사용자 동의 | 2026-09-29 |
| D-03 | RDB는 **MySQL 8.4 LTS** (`mysql:8.4.x`) | master 드라이버가 MySQL, 더미 스크립트가 MySQL 전용 문법, 팀 DDL 덤프도 MySQL 8.0.32. 8.0은 2026-04 EOL이라 8.4 LTS 선택 | 사용자 동의(MySQL) / 버전 Claude | 2026-09-29 |
| D-04 | main·batch가 **DB 하나(`findear`)를 공유** | 팀 batch가 main 테이블을 JPA로 직접 읽음 | 사용자 동의 | 2026-09-29 |
| D-05 | main·batch·match 모두 **Spring Boot 3.5.x**(현재 3.5.16)로 통일. batch는 2.7→3.5 마이그레이션. 4.x는 추후 | 2.7은 지원 종료, 3.5는 아직 패치 제공 중, 4.x는 Jackson 3 전환 등 작업량 큼 | 사용자 동의(3.x) / 3.5 선택 Claude | 2026-09-29 |
| D-06 | **Elasticsearch 8.19.x** 사용. Lost112 습득물과 매칭 결과(로그)는 ES에 저장 (팀 방식) | Boot 3.5가 관리하는 ES 클라이언트 8.18과 호환. 팀 batch 코드 재사용 | 사용자 동의 | 2026-09-29 |
| D-07 | **Spring Cloud Config 제거** → Spring 프로필(`local`/`prod`) + 환경변수(`.env`) | 설정 repo 유실, 1인 운영, 서비스 수 감소 (P2) | Claude 제안 → 사용자 동의 | 2026-09-29 |
| D-08 | **Jenkins 제거 → GitHub Actions** | public 레포라 표준 러너 무료. 사용자 경험상 한도 문제 없음 | 사용자 | 2026-09-29 |
| D-09 | Blue/Green 무중단 배포 제거 (필요 시 추후 재도입) | 로컬 실행 전제 (P5) | Claude 제안 → 사용자 동의 | 2026-09-29 |
| D-10 | **Nginx 게이트웨이 없음** (로컬·1차 배포 모두). 배포 시 main을 80에 직접 게시. 프론트 복구 시 프론트 컨테이너(nginx)가 정적 서빙 + `/api` 프록시를 겸함 | TLS 없음(P6), 프론트 없음(P3), 서버 간 호출은 compose 내부망, Blue/Green 없음 | Claude 설명 → 사용자 이의 없음 | 2026-09-29 |
| D-11 | HTTPS 적용하지 않음 | 사용자 요구 (P6) | 사용자 | 2026-09-29 |
| D-12 | 로컬 S3 대체재는 **SeaweedFS**(`chrislusf/seaweedfs`). 배포 시 **AWS S3로 설정만 전환** (AWS SDK v2 + endpoint override) | `minio/minio`, `minio/mc` 이미지가 Docker Hub에서 사라진 것을 확인(2026-09-29), bitnami/minio는 2025-09 이후 갱신 없음. SeaweedFS는 활발히 갱신, Apache-2.0, 단일 컨테이너로 가벼움. 후보: adobe/s3mock, Garage, RustFS | 사용자 동의 / 선택 Claude | 2026-09-29 |
| D-13 | 이미지 업로드는 **main이 presigned PUT URL 발급 → 클라이언트가 직접 업로드**. DB에는 **object key** 저장, 응답 시 URL 조립. 프론트에 IAM 키를 두지 않음 | 팀 시절 구조(브라우저에 IAM 키)는 보안 문제. key 저장은 스토리지·CDN 교체에 독립적 | Claude 제안 → 사용자 동의 | 2026-09-29 |
| D-14 | FCM 1차 검증용 **테스트 HTML 페이지**(`tools/fcm-test/`) 허용 | 실제 FCM 토큰은 브라우저에서만 발급 가능, 프론트는 1차 제외 | 사용자 | 2026-09-29 |
| D-15 | git 히스토리의 비밀값은 **폐기(rotate)만** 하고 히스토리 재작성은 하지 않음. HEAD에 남은 비밀값 파일은 삭제 | 사용자 결정 | 사용자 | 2026-09-29 |
| D-16 | `Chore/10-reset_env` 브랜치 삭제 | 사용자 결정. 내용은 [02](02-current-state.md#6-브랜치)에 요약 보존 | 사용자 | 2026-09-29 |
| D-17 | MongoDB 복구하지 않음 | 전체 히스토리 검색 결과 코드·의존성·compose 어디에도 없음 (문서에만 등장) | Claude | 2026-09-29 |
| D-18 | **모니터링: Prometheus + Grafana**. 수집 대상: Spring 앱 3개(Micrometer), MySQL(mysqld-exporter), Redis(redis_exporter), ES(elasticsearch-exporter), 컨테이너 리소스(cAdvisor), SeaweedFS(내장 metrics), 배포 시 호스트(node-exporter). compose profile `monitoring`, 기본 활성 | 사용자 요구 (P8) | 사용자(도입) / 구성 Claude | 2026-09-29 |
| D-19 | 컨테이너 **메모리 제한은 지정**, CPU 제한은 로컬에서 지정하지 않음. JVM은 `-XX:MaxRAMPercentage`, ES는 힙 고정 | 제한이 없으면 ES(가용 메모리의 약 50%)·JVM(25%)이 자동으로 크게 잡아 OOM 위험. CPU는 압축 가능 자원이라 제한 불필요, 제한 시 JVM 기동 지연 | Claude (사용자 질문 답변) | 2026-09-29 |
| D-20 | 스키마 관리는 **Flyway one-shot 컨테이너**(`flyway/flyway`), 마이그레이션은 `infra/db/migration/`. main·batch는 `ddl-auto: validate` | 현재 레포에 DDL이 없어 스키마 재현 불가. DB 공유 구조에서 특정 앱에 스키마 소유를 두지 않기 위함 | Claude | 2026-09-29 |
| D-21 | 앱 포트와 **actuator 관리 포트 분리**, 관리 포트는 호스트에 게시하지 않음 (Prometheus가 내부망에서 수집) | 배포 시 `/actuator/prometheus` 외부 노출 방지 | Claude | 2026-09-29 |
| D-22 | 레거시 삭제: `config/`, stub `batch/`, `old-servers/match/`, `infra/findear-infra-setting/`, `infra/git-settings/`, `.gitlab/`. `exec/`는 `docs/legacy/`·`infra/db/dummy/`로 이동 | 원본은 `old-master`에 영구 보존, 필요한 정보는 [01](01-legacy-inventory.md)에 정리 (P2) | Claude | 2026-09-29 |
| D-23 | 이미지 레지스트리는 **GHCR** (`ghcr.io/ehighg/findear-{main,batch,match}`). Actions가 빌드·푸시, 배포 서버는 pull만 | public 레포 무료, 작은 EC2에서 빌드 불필요 | Claude | 2026-09-29 |
| D-24 | Java 17 유지 (코드 `sourceCompatibility`, 런타임 이미지 `eclipse-temurin:17-jre`) | 변경 최소화. 21 전환은 선택(O-3) | Claude | 2026-09-29 |
| D-25 | Redis는 공식 `redis:8.x`, 영속화 없음 | refresh token만 저장. 라이선스는 자체 사용에 문제없음 (대안: Valkey) | Claude | 2026-09-29 |
| D-26 | 개발용 기능은 **`local` 프로필에서만 활성**: 전화번호 로그인, 테스트 가입, 알림 테스트 엔드포인트, batch의 전체삭제·테스트 API | 배포 시 백도어 방지 (P5) | Claude | 2026-09-29 |
| D-27 | 실행 모드 2가지 지원: (A) 전체 컨테이너 `docker compose up -d`, (B) 인프라만 컨테이너 + 앱은 IDE. 앱의 `local` 기본값은 localhost, compose가 환경변수로 서비스명 주입 | 개발 편의 | Claude | 2026-09-29 |
| D-28 | match mock은 **Spring Boot 3.5 경량 앱**(`match/`). 기존 API 경로·JSON 형태 유지, 점수 로직은 교체 가능한 전략 인터페이스 + 기본 구현(결정적 의사난수 + 간단한 가중치) | P1. 로직 세부는 사용자가 설계 예정(O-1) | Claude | 2026-09-29 |
| D-29 | 포트 체계(앱/관리): main 8080/8081, batch 8082/8083, match 8084/8085 | 실행 모드 (B)에서 한 호스트에 동시 실행 가능하도록 | Claude | 2026-09-29 |
| D-30 | GitHub Actions 무료 한도는 고려하지 않음 | public 레포 | 사용자 | 2026-09-29 |

## 미결 사항 (사용자 판단 필요)

| ID | 내용 | 언제 | 현재 기본값 |
|---|---|---|---|
| O-1 | match mock의 매칭 규칙(임의 로직) 세부 | R-40 착수 전/후 | [07](07-api-contracts.md#5-match-mock-동작-명세)의 기본 구현으로 먼저 진행 |
| O-2 | 배포 시 AWS 비용 (EC2 사양 — 모니터링 포함 시 메모리 8GB급 권장, S3) | 실제 배포 시점 | 배포하지 않음 (P5) |
| O-3 | Java 21 전환 여부 | 언제든 | Java 17 유지 |
| O-4 | Lost112 수집 운영값 (주기·기간·페이지 크기) | API 키 발급 후 트래픽 한도 확인 시 | 매일 04:00, 최근 30일, 1,000건/페이지 |
| O-5 | 프론트 재도입 시 batch 직접 호출 유지 여부 (main 경유 권장) | 프론트 복구 시 | – |
| O-6 | GitHub 이슈를 R-xx별로 미리 일괄 생성할지, 착수 시 생성할지 | 구현 착수 시 | 착수 시 생성 |
| O-7 | **HTTPS 미적용(P6)이면 배포 환경에서 웹푸시(FCM)가 동작하지 않음** (Service Worker·Push API는 보안 컨텍스트 필요, localhost만 예외). 배포 환경에서 웹푸시가 필요해지면 HTTPS 적용 여부 재결정 | 실제 배포 시점 | 로컬에서만 웹푸시 검증 |
