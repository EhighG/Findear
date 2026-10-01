# Grafana 대시보드

Grafana가 시작할 때 `provisioning/dashboards/findear.yml`이 이 폴더를 읽어 대시보드를 등록한다 (R-51). 폴더 이름이 Grafana 폴더 이름이 된다.
**파일이 기준**이다: UI에서 고쳐도 저장되지 않으니(`allowUiUpdates: false`) 바꾸려면 JSON을 고치고 30초 안에 반영된다.
데이터소스는 모두 `{"type":"prometheus","uid":"prometheus"}`(provisioning/datasources/prometheus.yml)를 쓴다.

| 폴더 | 파일 | uid | 내용 |
|---|---|---|---|
| `findear/` | `findear-overview.json` | `findear-overview` | 직접 만든 **Findear Overview** (서비스 상태, 앱 HTTP, 서버 간·외부 호출, 배치 잡, Lost112 수집, FCM, 자원, 데이터 저장소) |
| `imported/` | `jvm-micrometer-4701.json` | `findear-jvm` | JVM (Micrometer) |
| `imported/` | `spring-boot-3x-19004.json` | `findear-spring-boot` | Spring Boot 3.x Statistics (HikariCP·로그 포함) |
| `imported/` | `mysql-overview-7362.json` | `findear-mysql-overview` | MySQL (mysqld-exporter) |
| `imported/` | `redis-763.json` | `findear-redis` | Redis (redis_exporter) |
| `imported/` | `elasticsearch-14191.json` | `findear-elasticsearch` | Elasticsearch (elasticsearch-exporter) |
| `imported/` | `cadvisor-14282.json` | `findear-cadvisor-14282` | 컨테이너 (cAdvisor) |

배포 서버 전용 Node Exporter Full(1860)은 node-exporter를 붙이는 R-62에서 추가한다.

## 가져온 대시보드의 출처

받은 날 2026-10-01. 다운로드 주소 형식: `https://grafana.com/api/dashboards/{ID}/revisions/{리비전}/download` (각각 그 시점의 최신 리비전).

| 대상 | grafana.com ID | 리비전 | 작성자 | 페이지 |
|---|---|---|---|---|
| JVM (Micrometer) | 4701 | 10 | mweirauch | https://grafana.com/grafana/dashboards/4701 |
| Spring Boot 3.x Statistics | 19004 | 1 | pierreconstantial | https://grafana.com/grafana/dashboards/19004 |
| MySQL Overview | 7362 | 5 | nasskach | https://grafana.com/grafana/dashboards/7362 |
| Redis Dashboard for Prometheus Redis Exporter 1.x | 763 | 6 | oliver006 | https://grafana.com/grafana/dashboards/763 |
| Elasticsearch Exporter Quickstart and Dashboard | 14191 | 1 | Grafana Labs | https://grafana.com/grafana/dashboards/14191 |
| Cadvisor exporter | 14282 | 1 | kokorinav | https://grafana.com/grafana/dashboards/14282 |

각 대시보드의 라이선스·권리는 원 작성자에게 있다. 가져온 JSON은 아래 "손댄 곳"만 바꿨다.

## 후보가 둘이었던 대상: 고른 이유

판단 기준은 이 레포의 구성(Prometheus 3.15, Grafana 13.2.3, 수집 간격 15초, mysqld-exporter 0.20, cAdvisor 0.55, Docker Desktop)에서 패널에 실제로 값이 나오는가이다 (패널 쿼리를 Grafana `/api/ds/query`로 실행해 확인).

- **MySQL: 7362 선택, 14057 제외.** 14057(MySQL Exporter Quickstart, 리비전 1)은 `rate(...[$__interval])`를 써서 수집 간격(15초)과 같은 구간에는 샘플이 하나뿐이라 QPS·Questions 등 27개 중 16개 패널이 비었다. 7362는 `$interval`(1m 이상)을 써서 36개 중 25개에 값이 나온다.
- **cAdvisor: 14282 선택, 19792 제외.** 14282는 6개 패널이 모두 값이 나온다. 19792(cadvisor dashboard, 리비전 6, simonmysun)는 85개 중 22개(컨테이너 파일시스템·CFS 제한 패널)가 비었다 — Docker Desktop에서 `container_fs_*`가 수집되지 않기 때문이다(04 §6). 리눅스 배포 서버(R-62)에서 더 자세한 컨테이너 지표가 필요하면 19792를 추가하는 것을 검토한다.

## 손댄 곳 (원본에서 바꾼 것)

1. 모든 파일: `__inputs`·`__requires`·`__elements`(빈 값) 제거, `id`를 `null`로, `uid`를 위 표의 고정 값으로.
2. 데이터소스 참조 `${DS_PROMETHEUS}`·`${DS_PROM}`(JVM·MySQL·Redis·cAdvisor·Spring Boot)과 Spring Boot의 내장 uid(`W0lFOlOVk`)를 `{"type":"prometheus","uid":"prometheus"}`로 바꿈. (Elasticsearch 대시보드는 `$datasource` 변수를 쓰므로 그대로 둔다 — provisioning된 Prometheus가 선택된다.)
3. `elasticsearch-14191.json`의 "Cluster health" 패널 쿼리 `...color="yellow"}==1)+22` → `+2`: 원본은 green=5, yellow=3, red=1로 매핑하는데 yellow에만 22를 더해 "Yellow" 대신 숫자 23이 표시됐다 (단일 노드 ES는 복제본이 있는 색인에서 yellow가 정상).
4. 그 밖의 패널·쿼리·변수는 그대로다.

## 알려진 빈 패널 (정상)

앱·exporter가 해당 지표를 내지 않거나 이 구성에 없는 대상이라 값이 없는 패널이다.

- JVM(4701) "Utilisation"(Tomcat 스레드): `server.tomcat.mbeanregistry.enabled=true`가 없어 `tomcat_threads_*`를 내지 않음. "JVM Process Memory": 이 대시보드가 쓰는 `process_memory_*_bytes`를 Micrometer가 내지 않음.
- Spring Boot(19004) "Connection Creation Time": `hikaricp_connections_creation_seconds_sum / _count`가 값을 내지 않음(새 연결을 만들지 않은 구간).
- MySQL(7362): Query Cache(MySQL 8에는 없음), Process States(processlist 수집기 꺼짐), "Buffer Pool Size of Total RAM"·I/O Activity·Memory Distribution·CPU Usage / Load·Disk Latency·Network Traffic·Swap Activity(node-exporter 지표, R-62에서 node-exporter가 붙으면 채워짐).
- Redis(763) "Memory Usage": `maxmemory`가 없어 비율을 계산할 수 없음.
- Findear Overview·cAdvisor의 컨테이너 패널: 컨테이너를 재생성한 직후 약 5분 동안은 cAdvisor가 옛 컨테이너(`id`가 다름)의 시계열을 같은 `name`으로 남겨, 메모리·CPU 선이 같은 이름으로 두 개 보일 수 있다(오류 아님). "메모리 제한 대비 사용 비율"은 이 때문에 `on (id, name)`으로 짝을 짓는다(`on (name)`이면 그동안 쿼리가 실패함 — R-51 검증에서 발견).
- Elasticsearch(14191) "Indices:" 행 7개 패널: 색인별 지표라 exporter 플래그 `--es.indices`가 필요하다(기본 수집 안 함, 플래그는 바꾸지 않았다). 맨 위 "Tripped for breakers"는 차단된 브레이커가 없으면 비어 있다.

## 다시 받는 방법

```sh
# 예: JVM (Micrometer) 4701의 리비전 10
curl -sL https://grafana.com/api/dashboards/4701/revisions/10/download -o raw-4701.json
```

받은 파일에서 위 "손댄 곳" 1~2번 처리(`__inputs`·`__requires`·`__elements` 삭제, `id`를 null, `uid` 고정, 데이터소스 참조를 고정 uid로)를 하고 `imported/`에 저장한다. 새 리비전이 나오면 같은 방법으로 받아 이 표의 리비전을 갱신한다.
