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
| D-31 | 컨테이너 메모리 제한 **기본값은 최소 사양** ([04 §5](04-target-architecture.md#5-리소스-산정-메모리) "최소(기본값)" 열, 합계 약 3.6GB). **튜닝은 일반적인 사용 방식 안에서만**: JVM은 `MaxRAMPercentage=50`(컨테이너 표준 방식)만 지정하고 GC 방식은 바꾸지 않음, ES는 힙 512m 고정만 하고 기능(ML 등)은 끄지 않음, `GOMEMLIMIT` 같은 추가 튜닝 없음 | 사용자 요구(최소 사양 + 일반적 사용 방식 유지, 2026-09-30 보완). 값은 개발 중 실측 없이 산정, R-90에서 실측해 조정. OOM이 나면 해당 서비스만 "여유" 값으로 올림 | 사용자(최소 사양·튜닝 범위) / 값 Claude | 2026-09-30 |
| D-32 | **개발 중(R-00~R-80)에는 전체 구성요소 동시 기동과 자원 실측을 하지 않음**. compose 파일·스크립트는 전체 구성으로 모두 작성하고, 전체 구성은 `docker compose config`로 검증, 각 R-xx 동작 확인은 필요한 서비스만 부분 기동 후 `down`. **최종 검증(R-90)에서는 모니터링까지 전체를 띄워 검증하고 자원을 실측**한다 | 사용자 결정 (2026-09-30 보완) | 사용자 | 2026-09-30 |
| D-33 | 이슈 운영: 옛 이슈(#1~#11, 개인 리팩토링·이전 재세팅 시도)는 사용자가 모두 닫았으므로(2026-09-30 확인) 신경 쓰지 않고, 필요한 이슈는 새로 만든다. **새 상위 이슈**("Findear 복구 1차")를 만든다. **Phase마다 하위 이슈 1개**(GitHub sub-issue로 연결), **R-xx마다 브랜치 1개** `{feature\|fix\|test}/{Phase 이슈번호}-{이름}` (README의 "1이슈 : n브랜치"). 이슈는 Phase 착수 시 생성 | 사용자(옛 이슈 정리, 새 이슈 생성) / 단위 Claude 제안 → 사용자 동의. O-6 해결 | 사용자 | 2026-09-30 |
| D-34 | master 반영은 **Claude가 수행**: R-xx 완료 기준 통과 → 비밀값 검사 → 작업 브랜치 원격 push(세부 기록 보존) → 로컬에서 rebase로 커밋 본문에 `Related to #{Phase 이슈}` 추가 → master에 fast-forward 병합 → master push → 로컬 브랜치 삭제. PR은 쓰지 않음(README 규칙). **Phase가 끝날 때마다 멈추고 사용자에게 보고** | README 리팩토링 규칙 + "원격에는 master에서 분기한 브랜치만 push" 때문에 R-xx마다 master에 들어가야 다음 작업을 push할 수 있음 | Claude 제안 → 사용자 동의 | 2026-09-29 |
| D-35 | 세션 단위는 **Phase**. 세션 종료 시 08·10·README 갱신 | 컨텍스트가 길어지는 것 방지, 인계는 문서로 | Claude 제안 → 사용자 동의 | 2026-09-29 |
| D-36 | **원본 레포 `2TF4/findear`(이 레포의 포크 원본)에는 이슈·PR·push·코멘트 등 어떤 쓰기도 하지 않음.** 장치: ① 커밋된 `.claude/settings.json`에 `GH_REPO=EhighG/Findear`(모든 Claude 세션의 gh 대상 고정) ② 같은 파일에 `2TF4`/`2tf4`가 들어간 Bash·PowerShell 명령 deny ③ 로컬 clone에 `gh repo set-default EhighG/Findear` ④ `upstream` 등 원본 레포 remote를 추가하지 않음 | 사용자 계정이 원본 레포 admin이라 잘못 지정하면 실제로 반영됨 (2026-09-29 확인). gh는 포크에서 기본 레포가 없으면 대화형 실행 시 원본을 후보로 제시함 | 사용자(금지) / 장치 Claude | 2026-09-29 |
| D-37 | U-01(Naver Secret 재발급), U-02(Secret scanning·Push protection), U-05(Lost112 API 활용신청)는 **1차 작업 완료 후** 사용자가 진행. 그동안 외부 연동은 D-38 방식(공식 문서 기준 구현 + mock 검증, 실제 확인은 R-91)으로 진행하고, push 보호가 없으므로 **push 전 비밀값 검사를 Claude가 직접 수행** | 사용자 결정 | 사용자 | 2026-09-29 |
| D-38 | **외부 API는 작업·검증 중 호출하지 않는다.** 대상: Naver 로그인, VWorld, Firebase(FCM), 공공데이터포털 Lost112, AWS. 키 없이 보내는 요청도 포함. 공식 문서 열람은 허용하고, 이미지·의존성 다운로드와 이 레포의 GitHub 작업(이슈·push)은 해당하지 않음. 대신 **사용자가 마지막에 키만 세팅하면 바로 동작하도록** 완성한다: ① 공식 문서의 현재 명세 기준으로 구현하고 확인한 문서 URL·날짜를 [05](05-external-integrations.md)에 기록 ② 외부 API 주소는 설정값(기본값은 공식 주소)으로 두어 테스트에서 로컬 mock 서버로 교체 ③ 공식 문서의 요청 형식·응답 예시(오류 포함)를 재현한 mock 계약 테스트로 검증, FCM은 Firebase를 호출하지 않는 단위 테스트 ④ 키가 없어도 앱은 기동하고 해당 기능만 "설정 필요" 오류로 응답 ⑤ `.env.example`·[05](05-external-integrations.md) 키 세팅 체크리스트·확인 스크립트(`tools/verify-external/`) 제공. 실제 동작 확인은 키 세팅 후 사용자(R-91) | 사용자 결정 (키 미갱신) | 사용자 | 2026-09-30 |
| D-39 | master에 올리는 커밋의 이슈 참조 `Related to #N`은 **본문 마지막 문단, 트레일러(`Co-Authored-By:` 등) 앞**에 넣는다. 도구: `tools/git/add-issue-ref.sh` (`ISSUE_REF='Related to #N' git rebase -x 'sh tools/git/add-issue-ref.sh' master`, 이미 있으면 건너뜀) | 트레일러 뒤에 붙이면 트레일러가 메시지 끝 문단이 아니게 되어 git이 트레일러로 인식하지 않음(GitHub 공동 작성자 표시도 끝 문단의 `Co-authored-by` 트레일러 기준). 기존 커밋 관례(`Related to #N`을 본문 끝 별도 문단)와 같은 모양. R-xx마다 반복하는 절차라 스크립트로 고정 (2026-09-30 임시 레포에서 트레일러 유지·중복 방지 확인) | Claude | 2026-09-30 |
| D-40 | **R-11을 나눔**: R-11a(Phase 1) = Flyway one-shot 서비스 + Spring Batch 메타 스키마 `V1__spring_batch_schema.sql`, R-11b(Phase 2, R-20 다음) = main 스키마 `V2__init_schema.sql`(Boot 3.5 Hibernate로 생성) + 개발용 시드. main의 `validate` 기동 확인은 R-21 완료 기준으로 옮김. 마이그레이션 번호도 바꿈 (원래 V1=main, V2=batch 메타) | main 스키마는 R-20(Boot 3.5) 이후에만 만들 수 있고 main 기동에는 R-21(설정 외부화)이 필요해서 원래 R-11은 Phase 1 안에서 끝낼 수 없음. Flyway는 이미 적용된 버전보다 낮은 번호를 나중에 추가하면 검증에 실패하므로(`outOfOrder` 미사용) Phase 1에서 먼저 적용하는 batch 메타 스키마를 V1로 | 사용자(분할) / 방식 Claude | 2026-09-30 |
| D-41 | 테스트·검증에서 **AWS에 실제로 연결해야 하는 부분은 생략**한다. 로컬에서 같은 동작을 볼 수 있는 부분(SeaweedFS에 대한 S3 API 호출, `compose.prod.yml` config 검증)만 확인하고, AWS 전용 부분(IAM Role 자격증명, 버킷 정책·Public Access Block, EC2 호스트 초기화·배포, SSH 배포 워크플로)은 문법 검사(`bash -n`, JSON 문법 등)까지만 한다. 실제 확인은 사용자가 배포할 때(U-08) | 사용자 결정 | 사용자 | 2026-09-30 |
| D-42 | R-13(SeaweedFS)의 완료 기준은 aws-cli로 확인할 수 있는 것으로 둔다: `storage-init`(버킷·CORS), 서명 업로드, 익명 GET 허용·익명 PUT 거부, CORS preflight, presigned **GET**, 재기동 후 데이터 유지. **presigned PUT은 R-24**(main의 `POST /images/presign`, AWS SDK v2 `S3Presigner`)에서 처음 확인 | aws-cli `s3 presign`은 GET용 URL만 만든다 (AWS CLI 2.37.6 공식 문서, 2026-09-30 확인, [05 §8](05-external-integrations.md#8-공식-문서-확인-기록-d-38)). presigned PUT만 보려고 SDK 검증 도구를 따로 만들기보다 실제 사용 경로(R-24)에서 확인. 공개 엔드포인트 Host 분리([04 §2](04-target-architecture.md#주의할-설계-포인트))는 같은 SigV4 쿼리 서명을 쓰는 presigned GET으로 R-13에서 먼저 확인 | Claude | 2026-09-30 |
| D-43 | `.env.example`에는 **각 R-xx가 쓰기 시작하는 변수를 그때 추가**한다. [06 §6](06-db-and-config.md#6-환경변수-전체-목록)은 최종 목록이고 R-90에서 둘이 일치하는지 확인 | 아직 없는 서비스의 변수를 미리 넣어 두면 구현 중에 이름·기본값이 바뀔 때 어긋나기 쉬움. `.env.example`에는 실제로 쓰이는 변수만 둠 | Claude | 2026-09-30 |
| D-44 | 로컬 호스트 게시 포트는 `.env`의 `*_HOST_PORT`로 바꿀 수 있게 한다 (`compose.override.yml`). 기본값은 [04 §2](04-target-architecture.md#2-서비스-목록)의 포트(MySQL 3306, Redis 6379, ES 9200, SeaweedFS 8333), 모니터링·앱 포트도 해당 작업에서 같은 방식. 컨테이너 안 포트와 서비스 간 주소는 그대로 | 개발 PC에 Windows용 MySQL 8.0 서비스(`MySQL80`, 자동 시작)가 3306을 쓰고 있음 (2026-09-30 확인). 사용자 환경을 바꾸지 않고 충돌을 피하는 일반적인 방법. 이 PC의 `.env`는 `MYSQL_HOST_PORT=3307`이고, 실행 모드 B(IDE)에서는 앱의 `DB_PORT`도 같은 값으로 | Claude | 2026-09-30 |
| D-45 | 로컬 SeaweedFS의 공개 읽기는 anonymous identity가 아니라 **AWS와 같은 버킷 정책**으로 준다: `storage-init`이 `put-bucket-policy`로 `images/*`의 `s3:GetObject`만 `Principal: *`에 허용. `s3.json`에는 앱용 identity 하나만 둔다 | SeaweedFS 4.x가 버킷 정책(접두사 단위 리소스 포함)과 `put-bucket-cors`를 지원함 (SeaweedFS 위키 "S3 Bucket Policies", "Amazon S3 API", 2026-09-30 확인). anonymous identity는 버킷 단위(`Read:bucket`)까지만 돼서 06 §4의 "images/*만 공개"를 못 지킴. 로컬과 AWS가 같은 명령·같은 결과가 됨 (R-13에서 images/* 익명 GET 200, 그 밖의 경로 403 확인) | Claude | 2026-09-30 |

## 미결 사항 (사용자 판단 필요)

| ID | 내용 | 언제 | 현재 기본값 |
|---|---|---|---|
| O-1 | match mock의 매칭 규칙(임의 로직) 세부 | R-40 착수 전/후 | [07](07-api-contracts.md#5-match-mock-동작-명세)의 기본 구현으로 먼저 진행 |
| O-2 | 배포 시 AWS 비용 (EC2 사양 — 최소 사양 합계 약 3.6GB라 4GB급은 swap 필수로 빠듯, 여유 있게는 8GB급. S3) | 실제 배포 시점 | 배포하지 않음 (P5) |
| O-3 | Java 21 전환 여부 | 언제든 | Java 17 유지 |
| O-4 | Lost112 수집 운영값 (주기·기간·페이지 크기) | API 키 발급 후 트래픽 한도 확인 시 | 매일 04:00, 최근 30일, 1,000건/페이지 |
| O-5 | 프론트 재도입 시 batch 직접 호출 유지 여부 (main 경유 권장) | 프론트 복구 시 | – |
| O-7 | **HTTPS 미적용(P6)이면 배포 환경에서 웹푸시(FCM)가 동작하지 않음** (Service Worker·Push API는 보안 컨텍스트 필요, localhost만 예외). 배포 환경에서 웹푸시가 필요해지면 HTTPS 적용 여부 재결정 | 실제 배포 시점 | 로컬에서만 웹푸시 검증 |

해결된 미결 사항: O-6(이슈 생성 시점·단위) → D-33.
