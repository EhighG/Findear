# 04. 목표 구성 (Target Architecture)

> 결정 근거는 [03-decisions.md](03-decisions.md). 이미지 버전은 2026-09-29에 Docker Hub에서 확인한 최신 태그이며, 구현 시 최신 패치 버전으로 올려도 됩니다.

## 1. 구성도

```
개발자 PC (나중엔 EC2)
┌──────────────────────── docker compose (network: findear) ────────────────────────┐
│                                                                                   │
│  main :8080 ──HTTP──▶ batch :8082 ──HTTP──▶ match(mock) :8084                     │
│   │   └──── HTTP (/process, 습득물 자동채움) ──────▶ match(mock)                  │
│   ├──▶ mysql :3306 ◀── batch            batch ──▶ elasticsearch :9200            │
│   ├──▶ redis :6379                                                                │
│   └──▶ seaweedfs (S3 API :8333)   ← 배포 시 AWS S3로 설정만 교체                   │
│                                                                                   │
│  one-shot: flyway ──▶ mysql (스키마 마이그레이션)                                   │
│            storage-init ──▶ seaweedfs (버킷 생성, CORS)                            │
│                                                                                   │
│  [profile: monitoring]                                                            │
│   prometheus ◀── main:8081 / batch:8083 / match:8085  (/actuator/prometheus)      │
│              ◀── mysqld-exporter, redis-exporter, es-exporter, cadvisor, seaweedfs │
│   grafana ──▶ prometheus                                                          │
└───────────────────────────────────────────────────────────────────────────────────┘
외부 연동:  main → Naver OAuth, VWorld, FCM      batch → 공공데이터포털 Lost112 API ×2
클라이언트: curl / Postman / tools/fcm-test(브라우저, FCM 토큰 발급) → main :8080
```

- Nginx 없음(D-10), TLS 없음(D-11), Config Server 없음(D-07), Jenkins 없음(D-08).
- 서버 간 호출은 compose 내부망의 서비스 이름으로 합니다. URL은 전부 환경변수로 주입합니다.

## 2. 서비스 목록

### 상시 서비스
| 서비스 | 이미지 / 빌드 | 앱 포트 / 관리 포트 | 로컬 호스트 게시 | 의존 (condition) | 헬스체크 |
|---|---|---|---|---|---|
| `main` | `./main` 멀티스테이지 빌드 (`gradle:8.14-jdk17` → `eclipse-temurin:17-jre`) | 8080 / 8081 | `127.0.0.1:8080` | mysql(healthy), flyway(completed), redis(healthy), seaweedfs(healthy) | `GET :8081/actuator/health` |
| `batch` | `./batch` (동일 방식) | 8082 / 8083 | `127.0.0.1:8082` (디버깅용) | mysql(healthy), flyway(completed), elasticsearch(healthy) | `GET :8083/actuator/health` |
| `match` | `./match` (동일 방식, mock) | 8084 / 8085 | `127.0.0.1:8084` (디버깅용) | – | `GET :8085/actuator/health` |
| `mysql` | `mysql:8.4.11` | 3306 | `127.0.0.1:3306` | – | `mysqladmin ping` |
| `redis` | `redis:8.8.3` | 6379 | `127.0.0.1:6379` | – | `redis-cli ping` |
| `elasticsearch` | `elasticsearch:8.19.22` (single-node, 로컬은 security off) | 9200 | `127.0.0.1:9200` | – | `GET /_cluster/health?wait_for_status=yellow` |
| `seaweedfs` | `chrislusf/seaweedfs:4.48` (`weed server -s3`) | S3 8333, metrics 9327 | `127.0.0.1:8333` (presigned URL 업로드용) | – | S3 포트 응답 확인 |

### One-shot 서비스 (기동 시 1회 실행 후 종료)
| 서비스 | 이미지 | 역할 |
|---|---|---|
| `flyway` | `flyway/flyway:13.8.1` | `infra/db/migration/`의 SQL로 스키마 마이그레이션 (D-20) |
| `storage-init` | `amazon/aws-cli:2.37.5` | `--endpoint-url http://seaweedfs:8333`로 버킷 생성·CORS 적용. **AWS에서 쓸 명령과 동일한 명령**을 써서 로컬/배포 동작을 맞춤 |

### 모니터링 (compose profile `monitoring`, `.env`의 `COMPOSE_PROFILES=monitoring`으로 기본 활성)
| 서비스 | 이미지 | 포트 | 로컬 호스트 게시 |
|---|---|---|---|
| `prometheus` | `prom/prometheus:v3.15.0` | 9090 | `127.0.0.1:9090` |
| `grafana` | `grafana/grafana:13.2.3` | 3000 | `127.0.0.1:3000` |
| `cadvisor` | `gcr.io/cadvisor/cadvisor:v0.55.1` | 8080(내부) | – |
| `mysqld-exporter` | `prom/mysqld-exporter:v0.20.0` | 9104 | – |
| `redis-exporter` | `oliver006/redis_exporter:v1.92.1` | 9121 | – |
| `es-exporter` | `prometheuscommunity/elasticsearch-exporter:v1.11.0` | 9114 | – |
| `node-exporter` (배포 전용, `compose.prod.yml`) | `prom/node-exporter:v1.12.1` | 9100 | – |

### 주의할 설계 포인트
- **presigned URL 호스트**: SigV4 서명에 Host가 포함되므로, 서버 내부용 S3 클라이언트(`http://seaweedfs:8333`)와 **presigned URL 생성용 엔드포인트**(`http://localhost:8333`)를 분리해야 합니다 (`STORAGE_ENDPOINT` / `STORAGE_PUBLIC_ENDPOINT`). AWS에서는 둘 다 비워 기본 엔드포인트 사용.
- **SeaweedFS 자격증명**: `s3.json`에 키를 하드코딩하지 않도록 entrypoint에서 환경변수로 렌더링. 버킷 공개 읽기는 anonymous identity의 Read 권한으로 설정 (구현 시 SeaweedFS 문서로 확인).
- **헬스체크 도구**: `eclipse-temurin` JRE 이미지에 curl이 없을 수 있음 → 런타임 스테이지에서 설치하거나 wget 사용.
- **ES 로컬 설정**: `discovery.type=single-node`, `xpack.security.enabled=false`, `cluster.routing.allocation.disk.threshold_enabled=false`(개발 PC 디스크 여유가 적을 때 인덱스가 read-only 되는 것 방지). Linux 호스트는 `vm.max_map_count=262144` 권장.
- **MySQL 설정**: `--character-set-server=utf8mb4 --collation-server=utf8mb4_0900_ai_ci --default-time-zone=+09:00`, `TZ=Asia/Seoul`. `infra/mysql/initdb/`에 mysqld-exporter 계정 생성 스크립트 (최초 초기화 때만 실행됨).

## 3. 볼륨 / 네트워크

| 볼륨 | 마운트 경로 | 비고 |
|---|---|---|
| `mysql-data` | `/var/lib/mysql` | 팀 시절처럼 `/backup`에 마운트하는 실수 금지 |
| `es-data` | `/usr/share/elasticsearch/data` | 경로 오타 주의 |
| `seaweedfs-data` | `/data` | |
| `prometheus-data` | `/prometheus` | 보존 기간 `PROMETHEUS_RETENTION`(로컬 7d) |
| `grafana-data` | `/var/lib/grafana` | 대시보드·데이터소스는 provisioning 파일로 관리 |
| (Redis) | – | 영속화 없음 (D-25) |

- 네트워크: 단일 bridge 네트워크 `findear`.
- 비밀 파일: `./secrets/firebase-adminsdk.json`(git 제외)을 main에 읽기 전용 마운트 → `FCM_CREDENTIALS_PATH`.

## 4. compose 파일 구성과 실행 방법

| 파일 | 역할 |
|---|---|
| `compose.yml` | 공통 서비스 정의 (이미지 이름, 환경변수, 볼륨, 헬스체크, 메모리 제한, profile) |
| `compose.override.yml` | **로컬 전용, 자동 병합**. 앱 `build:` 컨텍스트, `127.0.0.1` 포트 게시 |
| `compose.prod.yml` | 배포 전용. GHCR 이미지 pull, main만 `80:8080` 게시, Redis·ES 비밀번호/보안 on, 로그 로테이션, `restart: unless-stopped`, node-exporter |

```bash
# 모드 A: 전체 컨테이너 (로컬)
cp .env.example .env            # 값 채우기
docker compose up -d --build    # compose.yml + compose.override.yml 자동 병합
docker compose ps               # 전부 healthy 확인

# 모드 B: 인프라만 컨테이너 + 앱은 IDE (앱은 local 프로필 기본값인 localhost로 붙음)
docker compose up -d mysql redis elasticsearch seaweedfs flyway storage-init

# 배포 (EC2)
docker compose -f compose.yml -f compose.prod.yml pull
docker compose -f compose.yml -f compose.prod.yml up -d
```

모드 B에서 Prometheus가 IDE에서 띄운 앱을 수집하려면 `host.docker.internal:8081` 등을 대상으로 하는 별도 scrape 설정이 필요합니다 (Linux는 `extra_hosts: host.docker.internal:host-gateway`). 선택 사항입니다.

## 5. 리소스 산정 (메모리)

- 결정: 메모리 제한은 지정, CPU 제한은 로컬에서 지정하지 않음 (D-19).
- 제한을 안 걸면: 컨테이너는 Docker가 쓸 수 있는 자원 전체를 나눠 씁니다 (Docker Desktop은 VM 한도 = 기본 호스트 메모리의 약 50%). 이때 ES는 가용 메모리의 약 절반, JVM은 25%까지 자동으로 잡아서 합이 한도를 넘으면 OOM으로 컨테이너가 죽습니다.
- 아래 값은 추정치입니다. 구현 후 `docker stats`로 측정해 조정하고 이 표를 갱신하세요.
- compose에서는 `deploy.resources.limits.memory: ${MAIN_MEM_LIMIT:-768m}`처럼 환경변수로 덮어쓸 수 있게 하고, **기본값은 "권장" 열**로 둡니다.

| 서비스 | 최소 | 권장(기본 설정값) | 비고 |
|---|---|---|---|
| main | 512MB | 768MB | `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=70` |
| batch | 512MB | 768MB | Lost112 수집을 페이지 단위로 바꾼다는 전제 (R-32) |
| match (mock) | 256MB | 384MB | |
| mysql | 512MB | 768MB | performance_schema ON (exporter 지표용) |
| redis | 64MB | 128MB | |
| elasticsearch | 1GB (힙 512m) | 1.5GB (힙 768m) | 힙은 컨테이너 메모리의 절반 이하로 고정 |
| seaweedfs | 128MB | 256MB | |
| **핵심 소계** | **약 2.9GB** | **약 4.5GB** | |
| prometheus | 256MB | 512MB | |
| grafana | 128MB | 256MB | |
| cadvisor | 128MB | 256MB | `--housekeeping_interval`, `--docker_only`로 부하 절감 |
| exporter 3종 | 각 32MB | 각 64MB | mysqld / redis / elasticsearch |
| **모니터링 소계** | **약 0.6GB** | **약 1.2GB** | |
| **합계** | **약 3.5GB** | **약 5.7GB** | one-shot(flyway, storage-init)은 기동 시에만 잠깐 사용 |

- Docker Desktop 메모리 설정: **최소 5GB, 권장 8GB** (Docker 자체 오버헤드 0.5~1GB 포함). 호스트는 16GB 권장.
- CPU: 제한 없음. 동시 기동(JVM 3개 + ES) 시 4코어 이상이면 무난.
- 배포 서버: 권장값 기준 8GB급 인스턴스 필요 → 비용은 배포 시점에 사용자 판단 (O-2).

## 6. 모니터링 설계 (D-18)

### 수집 대상
| job | 대상 | 경로 | 비고 |
|---|---|---|---|
| main | `main:8081` | `/actuator/prometheus` | Micrometer, `application` 태그 |
| batch | `batch:8083` | `/actuator/prometheus` | Spring Batch 5 지표(`spring_batch_job_*`, `spring_batch_step_*`) 포함 |
| match | `match:8085` | `/actuator/prometheus` | |
| mysql | `mysqld-exporter:9104` | `/metrics` | 전용 계정 `exporter` (PROCESS, REPLICATION CLIENT, SELECT) |
| redis | `redis-exporter:9121` | `/metrics` | |
| elasticsearch | `es-exporter:9114` | `/metrics` | |
| cadvisor | `cadvisor:8080` | `/metrics` | 컨테이너별 CPU·메모리·네트워크·디스크 |
| seaweedfs | `seaweedfs:9327` | `/metrics` | 플래그 이름은 구현 시 확인 |
| prometheus | `localhost:9090` | `/metrics` | |
| node (배포 전용) | `node-exporter:9100` | `/metrics` | 호스트 지표 |

### 앱 쪽 설정 (main, batch, match 공통)
```yaml
management:
  server:
    port: 8081            # batch 8083, match 8085
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      probes:
        enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
```
- 의존성: `spring-boot-starter-actuator`, `io.micrometer:micrometer-registry-prometheus`.
- HTTP client 지표(`http_client_requests_*`)를 받으려면 `RestTemplate`·`WebClient`·`RestClient`를 **Boot가 제공하는 Builder 빈으로 생성**해야 합니다 (K-09).
- 커스텀 지표(권장): `findear_lost112_ingest_items_total`(수집 건수), `findear_matching_requests_total`/`_seconds`(match 호출), `findear_fcm_send_total{result}`(FCM 발송 결과).
- main의 SecurityConfig는 이미 `/actuator/**`를 permitAll 하고 있음. 관리 포트는 호스트에 게시하지 않으므로 외부 노출 없음 (D-21).

### Grafana
- 데이터소스·대시보드는 provisioning 파일로 코드화: `infra/monitoring/grafana/provisioning/{datasources,dashboards}/`, 대시보드 JSON은 `infra/monitoring/grafana/dashboards/`.
- 가져올 대시보드 후보 (grafana.com ID, 구현 시 exporter 버전과 호환 확인):
  | 대상 | 후보 ID |
  |---|---|
  | JVM (Micrometer) | 4701 |
  | Spring Boot 3.x Statistics | 19004 |
  | MySQL (mysqld_exporter) | 14057, 7362 |
  | Redis (redis_exporter) | 763 |
  | Elasticsearch (elasticsearch_exporter) | 14191 |
  | cAdvisor (컨테이너) | 14282, 19792 |
  | Node Exporter Full (배포 전용) | 1860 |
- 직접 만들 대시보드: **Findear Overview** — 서비스 up/down, main HTTP 요청 수·p95 지연·5xx 비율(uri별), 외부 연동(Naver, VWorld, Lost112, FCM) 및 내부 호출(batch, match) client 지연·오류, 배치 잡 소요시간·성공/실패, Lost112 수집 건수, 컨테이너 메모리 사용량 대비 제한, ES 문서 수, MySQL 커넥션·QPS, Redis 메모리.
- 알림(Alerting)·로그 수집(Loki)은 1차 범위 외.

### 접근·보안
- 로컬: Prometheus `127.0.0.1:9090`, Grafana `127.0.0.1:3000` (관리자 계정은 `.env`).
- 배포: Prometheus·Grafana는 **호스트 포트로 게시하지 않고** SSH 터널로 접근 (`ssh -L 3000:localhost:3000 …`, compose.prod에서 `127.0.0.1` 바인딩). HTTP 평문 로그인 노출 방지.
- cAdvisor는 Docker Desktop(Mac/Windows)에서 일부 지표가 제한될 수 있음 → R-14에서 확인하고, 안 되면 결과를 이 문서에 기록.

## 7. 목표 디렉토리 구조

```
Findear/
├── CLAUDE.md                     # Claude Code 세션 지침
├── README.md
├── compose.yml                   # 공통
├── compose.override.yml          # 로컬(자동 병합)
├── compose.prod.yml              # 배포
├── .env.example                  # 환경변수 템플릿 (06 문서의 목록과 일치)
├── .gitignore / .gitattributes   # 루트 (신규)
├── secrets/                      # (git 제외) firebase-adminsdk.json 등
├── main/                         # 메인 API
├── batch/                        # 배치·검색 서버 (팀 batch 복원 + Boot 3.5)
├── match/                        # 매칭 mock 서버 (신규)
├── front/                        # 레거시 프론트 (1차 범위 외, 손대지 않음)
├── infra/
│   ├── db/migration/             # Flyway V*.sql
│   ├── db/seed/                  # 소량 개발용 시드
│   ├── db/dummy/                 # 대량 더미 스크립트 (exec/data에서 이동)
│   ├── mysql/initdb/             # exporter 계정 생성 등
│   ├── seaweedfs/                # s3.json 템플릿, entrypoint
│   ├── monitoring/prometheus/    # prometheus.yml
│   ├── monitoring/grafana/       # provisioning/, dashboards/
│   ├── deploy/                   # init-host.sh, deploy.sh
│   └── aws/                      # S3·IAM 연동 키트 (README, 스크립트, 정책 JSON)
├── tools/fcm-test/               # FCM 토큰 발급용 테스트 페이지
├── docs/restoration/             # 복구 문서 (이 폴더)
├── docs/legacy/                  # 포팅 매뉴얼, 시연 시나리오 (exec에서 이동)
└── .github/
    ├── ISSUE_TEMPLATE/
    └── workflows/                # ci.yml, images.yml, (deploy.yml)
```
