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
| `main` | `./main` 멀티스테이지 빌드 (`gradle:8.14.5-jdk17` → `eclipse-temurin:17.0.20.1_1-jre-noble`) | 8080 / 8081 | `127.0.0.1:8080` | mysql(healthy), flyway(completed), redis(healthy), seaweedfs(healthy) | `GET :8081/actuator/health` |
| `batch` | `./batch` (동일 방식) | 8082 / 8083 | `127.0.0.1:${BATCH_HOST_PORT:-8082}` (디버깅용) | mysql(healthy), flyway(completed), elasticsearch(healthy) | `GET :8083/actuator/health` |
| `match` | `./match` (동일 방식, mock) | 8084 / 8085 | `127.0.0.1:8084` (디버깅용) | – | `GET :8085/actuator/health` |
| `mysql` | `mysql:8.4.11` | 3306 | `127.0.0.1:3306` | – | `mysqladmin ping` |
| `redis` | `redis:8.8.3` | 6379 | `127.0.0.1:6379` | – | `redis-cli ping` |
| `elasticsearch` | `elasticsearch:8.19.22` (single-node, 로컬은 security off) | 9200 | `127.0.0.1:9200` | – | `GET /_cluster/health?wait_for_status=yellow` |
| `seaweedfs` | `chrislusf/seaweedfs:4.48` (`weed server -s3`) | S3 8333, metrics 9327 | `127.0.0.1:8333` (presigned URL 업로드용) | – | S3 포트 응답 확인 |

### One-shot 서비스 (기동 시 1회 실행 후 종료)
| 서비스 | 이미지 | 역할 |
|---|---|---|
| `flyway` | `flyway/flyway:13.8.1` | `infra/db/migration/`의 SQL로 스키마 마이그레이션 (D-20) |
| `storage-init` | `amazon/aws-cli:2.37.5` | `--endpoint-url http://seaweedfs:8333`로 버킷 생성·CORS·공개 읽기 정책(`images/*`) 적용. **AWS에서 쓸 명령과 동일한 명령**을 써서 로컬/배포 동작을 맞춤 (`infra/seaweedfs/storage-init.sh`, 엔드포인트를 비우면 AWS 기본값) |

### 모니터링 (compose profile `monitoring`, `.env`의 `COMPOSE_PROFILES=monitoring`으로 기본 활성)
| 서비스 | 이미지 | 포트 | 로컬 호스트 게시 |
|---|---|---|---|
| `prometheus` | `prom/prometheus:v3.15.0` | 9090 | `127.0.0.1:9090` |
| `grafana` | `grafana/grafana:13.2.3` | 3000 | `127.0.0.1:3000` |
| `cadvisor` | `gcr.io/cadvisor/cadvisor:v0.55.1` | 8080(내부) | – |
| `mysqld-exporter` | `prom/mysqld-exporter:v0.20.0` | 9104 | – |
| `redis-exporter` | `oliver006/redis_exporter:v1.92.1` | 9121 | – |
| `es-exporter` | `prometheuscommunity/elasticsearch-exporter:v1.11.0` | 9114 | – |
| `node-exporter` (배포 전용, `compose.prod.yml`) | `prom/node-exporter:v1.12.1` | 9100 (호스트 네트워크) | 호스트의 9100에 열림 — **보안그룹에서 열지 않는다**. Prometheus는 `host.docker.internal:9100`으로 수집. Linux 전용(Docker Desktop은 `rslave` 마운트 불가) |

### 주의할 설계 포인트
- **로컬 호스트 포트**: 위 표의 "로컬 호스트 게시" 포트는 기본값이고, `.env`의 `*_HOST_PORT`로 바꿀 수 있다 (D-44). 개발 PC는 MySQL을 `127.0.0.1:3307`에 게시 (Windows용 MySQL이 3306 사용).
- **presigned URL 호스트**: SigV4 서명에 Host가 포함되므로, 서버 내부용 S3 클라이언트(`http://seaweedfs:8333`)와 **presigned URL 생성용 엔드포인트**(`http://localhost:8333`)를 분리해야 합니다 (`STORAGE_ENDPOINT` / `STORAGE_PUBLIC_ENDPOINT`). AWS에서는 둘 다 비워 기본 엔드포인트 사용.
- **SeaweedFS 자격증명**: `s3.json`에 키를 하드코딩하지 않도록 entrypoint에서 환경변수로 렌더링 (`infra/seaweedfs/entrypoint.sh`). 버킷 공개 읽기는 anonymous identity가 아니라 AWS와 같은 **버킷 정책**으로 `images/*`만 공개 (D-45, `storage-init.sh`).
- **헬스체크 도구**: `eclipse-temurin:17.0.20.1_1-jre-noble`(Ubuntu noble)에는 curl 8.5.0이 기본으로 들어 있어 따로 설치하지 않는다 (R-20 확인). 런타임 이미지를 바꾸면 curl 유무를 다시 확인.
- **ES 로컬 설정**: `discovery.type=single-node`, `xpack.security.enabled=false`, `cluster.routing.allocation.disk.threshold_enabled=false`(개발 PC 디스크 여유가 적을 때 인덱스가 read-only 되는 것 방지), 힙 `ES_JAVA_OPTS=-Xms512m -Xmx512m`. 그 외 기능(ML 등)은 기본값 유지 (D-31). Linux 호스트는 `vm.max_map_count=1048576`(Elastic 공식 Docker 운영 문서의 현재 값, 배포 서버는 `init-host.sh`가 설정 — R-63).
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
| `compose.override.yml` | **로컬 전용, 자동 병합**. 앱 `build:` 컨텍스트, 앱 서비스 `pull_policy: build`(D-58 — `docker compose pull`은 앱을 건너뛰고, `up`할 때마다 앱 이미지를 다시 빌드(레이어 캐시로 빠름)해 컨테이너를 다시 만든다), `127.0.0.1` 포트 게시 |
| `compose.prod.yml` | 배포 전용(R-62). GHCR 이미지 pull(build 없음), **AWS S3만 사용**(seaweedfs·storage-init은 profile `local-storage`라 안 뜨고, main의 `STORAGE_ENDPOINT`·`STORAGE_PUBLIC_ENDPOINT`는 빈 값, depends_on은 `!override`로 교체), main·batch·match `SPRING_PROFILES_ACTIVE=prod` 고정(D-60), main만 `80:8080` 고정 게시(변수 없음), Prometheus·Grafana는 `127.0.0.1`에만 게시, Redis `requirepass`(`REDIS_PASSWORD` 필수)·ES security on(`ELASTIC_PASSWORD` 필수, HTTP 평문) — batch·exporter가 같은 비밀번호로 접속, 모든 서비스 로그 로테이션(json-file 10m×3), 상시 서비스 `restart: unless-stopped`(flyway 제외), node-exporter, Prometheus `scrape.d/prod`·Grafana `host/` 대시보드 마운트 |

> **1차 작업 범위 (D-32)**: 개발 중(R-00~R-80)에는 전체를 한 번에 띄우지 않습니다. 전체 구성은 `docker compose config`로 검증하고, 각 작업의 동작 확인은 필요한 서비스만 골라 띄운 뒤 `docker compose down`으로 내립니다. **최종 검증(R-90)에서 모니터링까지 전체를 띄웁니다.**

```bash
# 전체 구성 검증 (개발 중)
cp .env.example .env
docker compose config --quiet   # compose.yml + compose.override.yml 병합 결과 검증
docker compose -f compose.yml -f compose.prod.yml config --quiet

# 부분 기동 예 (개발 중): 이미지 업로드 확인
docker compose up -d --build mysql flyway redis seaweedfs storage-init main
docker compose down

# 모드 A: 전체 컨테이너 (로컬) — 최종 검증(R-90)에서 실행
docker compose up -d --build    # compose.yml + compose.override.yml 자동 병합
docker compose ps               # 전부 healthy 확인

# 모드 B: 인프라만 컨테이너 + 앱은 IDE (앱은 local 프로필 기본값인 localhost로 붙음)
docker compose up -d mysql redis elasticsearch seaweedfs flyway storage-init
cd main && SPRING_PROFILES_ACTIVE=local ./gradlew bootRun   # 루트 .env를 읽음. 8080이 이미 쓰이면 SERVER_PORT=8090 등

# 배포 (EC2)
infra/deploy/deploy.sh          # git pull → .env 검사 → 아래 두 명령 → ps (설정 검사만: --check, 롤백: --tag <SHA>)
docker compose -f compose.yml -f compose.prod.yml pull
docker compose -f compose.yml -f compose.prod.yml up -d
```

모드 B에서 앱의 `local` 프로필은 루트 `.env`를 설정으로 읽어서(`spring.config.import`) 비밀번호 등을 따로 넣지 않아도 되고, DB·Redis 포트는 `MYSQL_HOST_PORT`·`REDIS_HOST_PORT`를 따라간다 (R-21).

모드 B에서 Prometheus가 IDE에서 띄운 앱을 수집하려면 `host.docker.internal:8081` 등을 대상으로 하는 별도 scrape 설정이 필요합니다 (Linux는 `extra_hosts: host.docker.internal:host-gateway`). 선택 사항입니다.

## 5. 리소스 산정 (메모리)

- 결정: 메모리 제한은 지정, CPU 제한은 로컬에서 지정하지 않음 (D-19). **기본값은 최소 사양** (D-31).
- 제한을 안 걸면: 컨테이너는 Docker가 쓸 수 있는 자원 전체를 나눠 씁니다 (Docker Desktop은 VM 한도 = 기본 호스트 메모리의 약 50%). 이때 ES는 가용 메모리의 약 절반, JVM은 25%까지 자동으로 잡아서 합이 한도를 넘으면 OOM으로 컨테이너가 죽습니다.
- "최소"·"여유" 열은 처음 산정한 값이고, **"R-90 실측" 열이 최종 검증(2026-10-01)에서 전체를 띄워 잰 값**입니다 (D-32). 실측 결과 **최소 기본값으로 OOM·재시작 없이 동작**해 기본값은 그대로 둡니다 (D-62). OOM(`docker inspect`의 `OOMKilled: true`, exit 137)이 나면 해당 서비스만 "여유" 열 값으로 올리고 이 표를 고칩니다.
- R-90 측정 조건: 깨끗한 clone, 모니터링 포함 15개 서비스 동시 기동, 시나리오 전체(로그인·이미지·등록·매칭·Lost112 샘플·잡 2분 간격·쪽지·패널 검사) 후 기동 약 27분 시점까지. 값은 working set(`docker stats`·cAdvisor `container_memory_working_set_bytes`, 비활성 파일 캐시 제외)의 최대. Docker Desktop(WSL2, cgroup v2), 다른 프로젝트 컨테이너 2개가 함께 떠 있던 PC. **부하 테스트·장시간 추세는 재지 않았음.**
- 튜닝은 일반적인 사용 방식 안에서만 합니다 (D-31): GC 방식 변경, ES 기능 끄기, `GOMEMLIMIT` 같은 추가 조정은 하지 않습니다.
- compose에서는 `deploy.resources.limits.memory: ${MAIN_MEM_LIMIT:-512m}`처럼 환경변수로 덮어쓸 수 있게 하고, **기본값은 "최소(기본값)" 열**로 둡니다.
- 제한값은 상한입니다. 합계가 곧 실사용량은 아니며, 실제 사용량은 이보다 낮습니다.

| 서비스 | 최소(기본값) | 여유 | R-90 실측 최대 (제한 대비) | 설정 (일반적인 사용 방식) |
|---|---|---|---|---|
| main | 512MB | 768MB | 460MiB (90%, 빠듯) — 요청마다 조금씩 늘어남 | `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=50` → 힙 약 256MB, 나머지는 메타스페이스·코드 캐시·스레드 몫. GC는 JVM 기본값. 실측 때 힙 사용 83/247MiB, 비힙 184MiB |
| batch | 512MB | 768MB | 352MiB (69%) | main과 같음. Lost112 수집을 페이지 단위로 바꾼다는 전제 (R-32) — 실측은 수집 off |
| match (mock) | 256MB | 384MB | 179MiB (70%) | main과 같음 → 힙 약 128MB |
| mysql | 512MB | 768MB | 500MiB (98%, 빠듯) — 평탄 | 기본 설정 (InnoDB buffer pool 128MB), performance_schema ON(기본값, exporter 지표용). 프로세스 메모리(anon) 479MiB(94%) |
| redis | 64MB | 128MB | 13MiB (21%) | 영속화 없음 (refresh token만 저장) |
| elasticsearch | 1GB | 1.5GB | 993MiB (97%, 빠듯) — 평탄 | 힙 `-Xms512m -Xmx512m` 고정 (Elastic 권장대로 힙 ≤ 컨테이너 메모리의 절반). 기능은 기본값 유지. 실측: 고정 힙 + 비힙 233MiB + launcher JVM 약 107MiB(ML controller는 7MiB) — 늘어나는 사용이 아니라 고정 할당이 대부분. anon 950MiB(93%) |
| seaweedfs | 128MB | 256MB | 111MiB (87%) — 업로드 뒤 늘어남 | 기본 설정 |
| **핵심 소계** | **약 2.9GB** | **약 4.5GB** | **약 2,610MiB** | |
| prometheus | 256MB | 512MB | 240MiB (94%, 빠듯) — 쿼리 부하 때 | scrape 15s, 보존 7d. Prometheus 3.x는 기본으로 `GOMEMLIMIT`를 메모리 제한×0.9로 자동 설정(추가 튜닝 아님, D-31) → 90% 근처에 머무는 것이 정상. 실측 시계열 약 11,000개(히스토그램 버킷 약 4,000, main이 가장 많음) |
| grafana | **512MB** | 768MB | 395MiB (77%) | 기본 설정. **R-51에서 192MB → 512MB (D-57)**: 대시보드를 열면 heap이 쌓여 256MB·384MB에서도 OOM, Grafana 공식 최소 권장 512MB. 실측 때 anon은 약 230MiB이고 나머지는 파일 캐시(제한에 닿아 회수 반복 — 정상) |
| cadvisor | 128MB | 256MB | 124MiB (97%, 빠듯) — 85~124 사이 변동 | `--docker_only=true --housekeeping_interval=30s` (cAdvisor 문서에 나오는 일반적인 부하 절감 옵션). 실측 PC는 다른 프로젝트 컨테이너까지 수집하므로 전용 서버에서는 더 낮을 수 있음 |
| exporter 3종 | 각 32MB | 각 64MB | 11 / 13 / 20MiB (34·41·62%) | mysqld / redis / elasticsearch |
| **모니터링 소계** | **약 1.0GB** | **약 1.7GB** | **약 800MiB** | |
| **합계** | **약 3.9GB** | **약 6.2GB** | **약 3,410MiB (제한 합계 4,000MiB의 85%)** | one-shot(flyway, storage-init)은 기동 시에만 잠깐 사용. OOMKilled·재시작 0, swap 사용 거의 없음(MySQL·Prometheus 각 88KB) |

- **빠듯한 서비스(최대가 제한의 90% 이상)**: ES·MySQL(고정 할당이라 평탄 — 데이터가 늘면 가장 먼저 위험), Prometheus(GOMEMLIMIT로 90% 근처가 정상), cAdvisor, main(경계, 요청마다 조금씩 증가). 로컬 개발은 기본값으로 충분했고, **오래 켜 두는 환경·배포 서버에서는 이 다섯 서비스를 "여유" 열 값으로 두는 것을 권장**한다 — `.env`의 `ES_MEM_LIMIT=1536m`, `MYSQL_MEM_LIMIT=768m`, `PROMETHEUS_MEM_LIMIT=512m`, `CADVISOR_MEM_LIMIT=256m`, `MAIN_MEM_LIMIT=768m`(ES 힙 `ES_JAVA_OPTS`는 512m 그대로 둬도 비힙 여유가 생김). 제한 합계는 약 5.3GB가 된다 (D-62).

- JVM 힙 비율을 70%가 아니라 50%로 두는 이유: 512MB에서 70%면 힙 358MB + 비힙 약 200MB로 제한을 넘을 수 있어 컨테이너가 OOM으로 종료됩니다. `MaxRAMPercentage`는 컨테이너에서 JVM 메모리를 맞추는 표준 방법입니다.
- 최소값에서 가장 빠듯할 수 있는 곳(산정 때 예상): ES(ML 기능이 기본으로 켜져 있어 별도 프로세스가 뜸), MySQL(performance_schema). → R-90 실측: 둘 다 제한의 97~98%로 빠듯했지만 OOM은 없음. ES의 ML 프로세스는 7MiB로 작고, 크기는 고정 힙과 launcher JVM 몫.
- Docker Desktop 메모리 설정: 제한 합계 3.9GB + Docker 자체 오버헤드 0.5~1GB → **최소 5GB, 여유 있게 7GB**. (현재 개발 PC는 16GB로 설정되어 있음, 2026-09-29 확인)
- CPU: 제한 없음. 참고로 JVM 3개와 ES를 동시에 기동하면 순간적으로 CPU를 많이 쓰므로 4코어 이상이면 무난합니다. 유휴 상태에서는 작습니다.
- 배포 서버: 최소값 기준 약 3.9GB + OS → 4GB급은 swap(2GB 이상)을 둬야 겨우 기동하는 수준이고, 여유 있게는 8GB급. 비용은 배포 시점에 사용자 판단 (O-2).

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
| node (배포 전용) | `host.docker.internal:9100` | `/metrics` | 호스트 지표. node-exporter가 호스트 네트워크라 서비스 이름이 아니라 호스트 주소로 수집 (`scrape.d/prod/node.yml`, prometheus에 `extra_hosts: host.docker.internal:host-gateway`) |

- Prometheus 설정 분리 (R-62): `prometheus.yml`은 공통 job만 두고 `scrape_config_files: [/etc/prometheus/scrape.d/*.yml]`로 환경별 job을 읽는다. 로컬 `compose.yml`은 `scrape.d/local/`(seaweedfs), 배포 `compose.prod.yml`은 같은 컨테이너 경로에 `scrape.d/prod/`(node)를 마운트한다.

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
- 커스텀 지표 (R-50 구현, 모든 태그 조합을 기동 때 0으로 등록): `findear_lost112_ingest_runs_total{service=POLICE|PORTAL, result=success|failure}`(서비스별 수집 실행 — policeJob은 수집이 실패해도 잡이 COMPLETED라 수집 실패는 이것과 스텝 상태로 봄, D-55), `findear_lost112_ingest_items_total{service, outcome=indexed|skipped}`, `findear_fcm_send_total{result=sent|skipped|token_invalid|failed}`. match 호출 수·지연은 `http_client_requests_seconds{client_name="match"}`로 충분해 `findear_matching_requests_*`는 만들지 않음.
- 앱 지표에는 앱 태그 `application`(main·batch·match)과 Prometheus `job` 레이블이 함께 붙는다. HTTP 클라이언트 `uri` 태그는 URI 템플릿(main→batch는 batch 전용 RestTemplate + 템플릿, R-50)이라 id·검색어가 들어가지 않는다. batch Lost112 호출은 `uri=none`(키가 태그에 들어갈 위험을 피함).
- batch: Spring Batch 5.2의 같은 이름 지표 중복 등록(이슈 spring-batch#4753, `spring.batch.job.active`)으로 첫 잡 실행 때 나던 Micrometer WARN은 `BatchMetricsConfig`의 `MeterFilter`로 관측 쪽만 막음(Batch 6/Boot 4로 올리면 제거).
- main의 SecurityConfig는 이미 `/actuator/**`를 permitAll 하고 있음. 관리 포트는 호스트에 게시하지 않으므로 외부 노출 없음 (D-21).

### Grafana
- 데이터소스·대시보드는 provisioning 파일로 코드화: `infra/monitoring/grafana/provisioning/{datasources,dashboards}/`, 대시보드 JSON은 `infra/monitoring/grafana/dashboards/`.
- 가져온 대시보드 (R-51, `infra/monitoring/grafana/dashboards/imported/`, 출처·리비전·손댄 곳·알려진 빈 패널은 그 폴더의 `README.md`):
  | 대상 | 고른 ID (후보) | 이유 |
  |---|---|---|
  | JVM (Micrometer) | **4701** | |
  | Spring Boot 3.x Statistics | **19004** | |
  | MySQL (mysqld_exporter) | **7362** (14057) | 14057은 `rate(...[$__interval])`가 수집 간격과 같아 패널 절반이 빔 |
  | Redis (redis_exporter) | **763** | |
  | Elasticsearch (elasticsearch_exporter) | **14191** | 원본 Cluster health 쿼리의 yellow 값 버그(`+22`) 수정. "Indices:" 행은 exporter `--es.indices`가 필요해 빔(플래그는 기본값 유지) |
  | cAdvisor (컨테이너) | **14282** (19792) | 19792는 Docker Desktop에 없는 `container_fs_*`·CFS 지표에 의존 — 리눅스 배포 서버에서는 R-62에서 재검토 |
  | Node Exporter Full (배포 전용) | **1860** | `dashboards/host/`, `compose.prod.yml`만 마운트 (로컬에는 보이지 않음). uid `findear-node` |
  - provisioning: `provisioning/dashboards/findear.yml`(파일 provider, 폴더 구조 = Grafana 폴더 `findear`·`imported`, UI 수정 불가), 데이터소스는 uid `prometheus`로 고정.
- 직접 만든 대시보드 (R-51, `dashboards/findear/findear-overview.json`, uid `findear-overview`, 변수 `application`): **Findear Overview** — 서비스 up/down, main HTTP 요청 수·p95 지연·5xx 비율(uri별), 외부 연동(Naver, VWorld, Lost112, FCM) 및 내부 호출(batch, match) client 지연·오류, 배치 잡 소요시간·성공/실패, Lost112 수집 건수, 컨테이너 메모리 사용량 대비 제한, ES 문서 수, MySQL 커넥션·QPS, Redis 메모리.
  - 행: 서비스 상태 / 앱 HTTP 서버(요청 수·p95·5xx 비율) / 서버 간·외부 호출(`client_name`별) / 배치 잡(잡 실행 수·평균 소요·**스텝 비정상 종료 수**·실행 중 잡 — policeJob 수집 실패는 잡 상태가 아니라 여기서 보임, D-55) / Lost112 수집 / 알림(FCM) / 자원(컨테이너 메모리·제한 대비 비율·CPU·JVM 힙) / 데이터 저장소. 범위 안 증가량은 `clamp_min(sum(x) - (sum(x offset $__range) or sum(x)*0), 0)`(잡 시리즈가 첫 실행 때 생겨 `increase`가 첫 실행분을 놓치는 문제), 메모리 비율은 `on (id, name)`(컨테이너 재생성 직후 옛 시계열과 겹침)
- p95 패널용 히스토그램: 앱 `management.metrics.distribution.percentiles-histogram.http.server.requests: true`(세 앱), `http.client.requests: true`(main·batch). 시계열 하나(uri·method·status 조합)마다 버킷 69개 — 부분 기동에서 main 서버 690개(main 지표의 55%), R-90에서 전체 시계열 수를 기록
- 검증 팁: 같은 시리즈가 수집 간격(15s)보다 짧은 구간에 처음 몰려 생기면 `rate`·`increase`가 비어 보인다 — 확인용 트래픽은 15초 이상 간격으로.
- 알림(Alerting)·로그 수집(Loki)은 1차 범위 외.

### 접근·보안
- 로컬: Prometheus `127.0.0.1:9090`, Grafana `127.0.0.1:3000` (관리자 계정은 `.env`).
- 배포: Prometheus·Grafana는 **호스트 포트로 게시하지 않고** SSH 터널로 접근 (`ssh -L 3000:localhost:3000 …`, compose.prod에서 `127.0.0.1` 바인딩). HTTP 평문 로그인 노출 방지.
- cAdvisor는 Docker Desktop(Mac/Windows)에서 일부 지표가 제한될 수 있음 → R-14에서 확인 (2026-09-30, Windows 11 + Docker Desktop WSL2, cgroup v2): 컨테이너 이름으로 구분되고 CPU·메모리(working set·usage·limit)·네트워크·블록 I/O는 수집됨. 컨테이너별 파일시스템 사용량(`container_fs_usage_bytes`)은 수집되지 않음. `/etc/machine-id` 없음 경고는 무시해도 됨.

## 7. 목표 디렉토리 구조

```
Findear/
├── CLAUDE.md                     # Claude Code 세션 지침
├── .claude/settings.json         # Claude Code 공유 설정: GH_REPO 고정, 원본 레포 대상 명령 차단 (D-36)
├── .claude/agents/               # 역할 분담 subagent: findear-executor(실행), findear-verifier(검증) (D-46)
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
│   ├── elasticsearch/seed/       # Lost112 샘플 문서 + 적재 스크립트 (API 키 발급 전 조회 확인용, R-32)
│   ├── seaweedfs/                # s3.json 템플릿, entrypoint
│   ├── monitoring/prometheus/    # prometheus.yml, scrape.d/{local,prod}/ (환경별 수집 대상, R-62)
│   ├── monitoring/grafana/       # provisioning/, dashboards/{findear,imported,host}/ (host는 배포 전용)
│   ├── deploy/                   # init-host.sh, deploy.sh
│   └── aws/                      # S3·IAM 연동 키트 (README, 스크립트, 정책 JSON)
├── tools/fcm-test/               # FCM 토큰 발급용 테스트 페이지
├── tools/verify-external/        # 키 세팅 후 외부 연동 확인 스크립트 (R-81, 사용자가 R-91에서 실행)
├── tools/git/                    # master 반영 시 커밋에 이슈 참조를 붙이는 rebase 보조 스크립트 (D-39)
├── docs/restoration/             # 복구 문서 (이 폴더)
├── docs/legacy/                  # 포팅 매뉴얼, 시연 시나리오 (exec에서 이동)
└── .github/
    ├── ISSUE_TEMPLATE/
    ├── workflows/                # ci.yml(자동, 테스트만), images.yml·deploy.yml(수동 실행만, D-58)
    └── ci/                       # CI 전용 Gradle init 스크립트 (실패 테스트 로그)
```
