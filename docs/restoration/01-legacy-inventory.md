# 01. 팀 종료 시점 인프라 인벤토리 (`2af1413`, 2024-05-23)

> 원본은 `old-master` 브랜치(= `2af1413`)에 그대로 남아 있습니다. 파일 경로는 모두 그 시점 기준입니다.
> 비밀값은 이 문서에 옮기지 않았습니다 (위치만 기록).

## 1. 당시 아키텍처 (코드 기준으로 재구성)

```
Browser (React PWA)
 │ HTTPS :443  j10a706.p.ssafy.io
 ▼
EC2 (Ubuntu 20.04) ── docker compose ──────────────────────────
 web (nginx, TLS 종단)
  ├─ /        → front-dev      (nginx + React dist)
  ├─ /api     → main-dev-0|1   :8080   (/api/alarm = SSE 전용 설정)
  ├─ /batch   → batch-dev-0|1  :8082
  ├─ /match   → match-dev-0|1  :8000
  └─ /config  → config         :9000
 main   → mariadb-dev, redis-dev, config
 main   → batch, match   (내부망이 아니라 공개 도메인을 거쳐 호출)
 batch  → mariadb-dev, elastic-search-dev, config
 batch  → match          (공개 도메인 경유)
 config → SSAFY GitLab: findear-config repo
 jenkins (docker.sock 마운트) → dev-deploy.sh (Blue/Green)
────────────────────────────────────────────────────────────────
외부 연동
 front → S3(브라우저에서 직접 업로드), Firebase JS(FCM 토큰), Kakao Map/우편번호, Naver 로그인
 main  → FCM(firebase-admin), Naver OAuth API, VWorld
 batch → 공공데이터포털 Lost112 API 2종
 match → OpenAI GPT-4 Vision, KATS 색채 사이트(Selenium 크롤링), fastText 모델 파일
```

## 2. 호스팅·네트워크·배포

| 구성요소 | 내용 | 근거 파일 |
|---|---|---|
| AWS EC2 | SSAFY 할당 서버, Ubuntu 20.04.6, 도메인 `j10a706.p.ssafy.io` | `exec/포팅 매뉴얼.md` |
| Docker / Compose | 25.0.4 / 2.24.7. compose 1개, 프로젝트 경로 `/var/jenkins_home/findear-infra-setting` | `infra/findear-infra-setting/compose.yml` |
| Nginx (web) | 1.25.4. TLS 종단, 경로 라우팅, Blue/Green 업스트림 교체(`sed` + reload) | `web/conf/default.conf`, `web/conf/change-bg.sh` |
| TLS 인증서 | SSAFY 와일드카드 `*.p.ssafy.io` (Let's Encrypt, 2024-05-26 만료). 서버 간 호출이 공개 도메인을 거쳐서 main·batch 이미지 JVM truststore에 `output.cert` import | `web/cert/`, main·batch Dockerfile |
| UFW | 22, 80, 443, 3100, 8000, 8989 허용 (8989 용도 불명). Docker 게시 포트는 UFW를 우회하므로 DB/ES/Redis 포트가 외부에 열려 있었을 가능성 | 포팅 매뉴얼 |
| Jenkins | `jenkins/jenkins:jdk17` + docker CLI + JDK17 + Node 20, `docker.sock`·`/var/jenkins_home` 마운트, 3000:8080. **Job/Pipeline 정의는 레포에 없음(유실)** | `jenkins/Dockerfile` |
| 무중단 배포 | `dev-deploy.sh {main\|batch\|match}`: -0/-1 쌍으로 green 기동 → docker healthcheck → nginx 업스트림 교체 → 공개 도메인 `/actuator/health` 확인 → blue 종료. front·config·DB는 대상 아님 | `dev-deploy.sh` |
| 빌드 산출물 | Jenkins가 빌드한 jar/dist, match 소스+`model.bin`을 각 하위 디렉토리에 넣고 `docker compose build` | 포팅 매뉴얼 |

## 3. 컨테이너 (compose.yml)

| 컨테이너 | 이미지 / 런타임 | 호스트 포트 | 역할 |
|---|---|---|---|
| web | nginx 1.25.4 | 80, 443 | 리버스 프록시 |
| front-dev | nginx 1.25.3 + Vite 빌드 결과물 | – | PWA 정적 서빙 |
| main-dev-0/1 | ubuntu:focal + JRE17, Spring Boot 3.2.3 | – | 메인 API |
| batch-dev-0/1 | ubuntu:focal + JRE17, Spring Boot 2.7.12 | – | Spring Batch, Lost112 수집·검색, 매칭 조율 |
| match-dev-0/1 | python:3.12 + Poetry + Django(`runserver`) + Chrome 123 | 3230 (-1만) | AI 분석, 유사도 매칭 |
| config | Spring Cloud Config Server (Boot 3.2.3) | – | 설정 서버 |
| mariadb-dev | mariadb:10.11.7-jammy | 3300 | RDB, DB 이름 `main`, main·batch 공유 |
| redis-dev | redis:7.2.4 | 3303 | Refresh Token 저장 |
| elastic-search-dev | elasticsearch:7.17.3, 단일 노드, 힙 2g | 3301 | Lost112 습득물 검색, 매칭 로그 |
| jenkins | 위 표 | 3000, 50000 | CI/CD |
| test | `./test` (디렉토리 없음) | 8999 | match 테스트용으로 추정 |

## 4. 서버별 연결 관계

**Main** (Spring Boot 3.2.3, Java 17)
- MariaDB, Redis(Lettuce; key=memberId → refresh token, TTL 1439분), Config Server client(`optional:configserver:http://…@config:9000`, profile `dev`)
- JWT(access 2시간) + Spring Security, Naver OAuth2, FCM 발송(`firebase-admin 7.1.1`), SSE(`/alarm/subscribe/{memberId}`)
- VWorld 프록시(`/location/search`, `/location/address`), 키는 코드에 하드코딩
- batch·match 호출 URL 하드코딩(`https://j10a706.p.ssafy.io/batch|match`, 6개 파일 7곳)
- `spring-boot-starter-mail`은 의존성만 있고 미사용(이메일 인증 엔티티는 2024-03-22 삭제)

**Batch** (Spring Boot 2.7.12)
- MariaDB(main과 같은 DB의 테이블을 JPA로 직접 읽음), Elasticsearch(`RestHighLevelClient`), Config client, Spring Batch + `@Scheduled`
- `policeJob`: 매일 04:00. 종료 시점엔 수집 step 주석 처리 → 매칭 step만 실행
- `findearJob`: 2시간마다 Findear 분실물↔습득물 매칭
- Lost112 수집은 `POST /batch/search/save` 수동 실행 (ES 인덱스 전체 삭제 후 재적재)
- match 호출: `/match/matching/findear`, `/match/matching/lost` (공개 도메인 경유, 하드코딩 4곳)
- FCM 코드는 주석 처리 (알림은 main에서만)

**Match** (Django, Python 3.12, Poetry) — 이번 복구에서는 mock으로 대체 (P1)
- OpenAI `gpt-4-vision-preview`: 사진 URL + 물품명 → 카테고리/색상/키워드 JSON (`/process`). 이 모델은 현재 지원 종료
- fastText `model.bin`(git 미포함, 어떤 모델인지 기록 없음), Kiwi 형태소 분석
- Selenium + headless Chrome으로 국가기술표준원 한국색채 사이트 크롤링 → 색상 유사도, `colorDict.pickle` 캐시
- `police_portal_list.csv`(경찰관서·포털기관 4,513곳 위경도)로 거리 가중치
- `DEBUG=True`, SQLite(미사용)

**Config Server** (Spring Cloud Config)
- 백엔드: SSAFY GitLab private repo `lab.ssafy.com/yee950419/findear-config.git` (master). `main-dev`, `batch-dev` 설정과 비밀값 보관 → **유실**
- HTTP Basic 인증(in-memory 유저), nginx `/config`로 외부 노출돼 있었음

**Front** (React 18 + Vite 5 + TS, PWA) — 1차 범위 외 (P3)
- `VITE_BASE_URL`(=/api), `VITE_BATCH_URL`(=/batch)를 직접 호출
- PWA: `manifest.json`, `ServiceWorker.js`, `firebase-messaging-sw.js`

## 5. 외부 서비스 / API

| 서비스 | 호출 주체 | 용도 | 키 위치(당시) | 종료 시점 상태 |
|---|---|---|---|---|
| FCM (Firebase 프로젝트 `findear-bfd63`) | front: JS SDK + service worker / main: admin SDK | 웹푸시: 분실물 매칭 완료, 쪽지 수신·답장. 토큰 `tbl_notification`, 이력 `tbl_alarm` | 서비스계정 JSON `main/src/main/resources/key/` (1506901에서 삭제), VAPID는 front `.env` | 사용 중 |
| Lost112 (공공데이터포털, 경찰청 1320000) | batch | `LosfundInfoInqireService/getLosfundInfoAccToClAreaPd`(경찰관서 습득물), `LosPtfundInfoInqireService/getPtLosfundInfoAccToClAreaPd`(포털기관 습득물). XML, `numOfRows=30000` | config repo `my.secret-key` | 사용 중 |
| OpenAI | match | 이미지 분석 | `infra/findear-infra-setting/match/.env` | 사용 중 |
| AWS S3 `findearbucket` (ap-northeast-2) | front가 aws-sdk v2로 직접 업로드 | 이미지 저장. OpenAI가 URL로 읽으므로 공개 읽기 전제 | front `.env` `VITE_S3_*` (IAM 키가 번들에 포함되는 구조) | 사용 중 |
| Naver 로그인 | front(authorize URL 생성, **client_secret 포함**) + main(token 교환, `/v1/nid/me`) | 소셜 로그인 | config repo, front `.env` | 사용 중 |
| Kakao Maps JS SDK(+services) + Daum 우편번호 | front | 시설 검색, 지오코딩, 주소 입력 | `front/index.html` 하드코딩 | 사용 중 |
| VWorld | main 프록시 ← front | 장소 검색, 주소→좌표 | main `LocationController` 하드코딩 | 사용 중 |
| KATS 한국색채 웹사이트 | match (Selenium) | 색상 정보 크롤링 | – | 사용 중 |
| 소상공인 상가정보 API (B553077/sdsc2) | front | 시설 정보 | front `.env` | 2024-03-31 이후 미사용 |
| SSE (자체) | front ↔ main | 로그인 시 연결 | nginx `/api/alarm` | 연결만 하고 실제 이벤트는 테스트 엔드포인트에서만 발송 |

## 6. 데이터 저장소

- **MariaDB `main`**: 13개 테이블 — `tbl_member`, `tbl_agency`, `tbl_board`, `tbl_lost_board`, `tbl_acquired_board`, `tbl_img_file`, `tbl_scrap`, `tbl_lost112_scrap`, `tbl_return_log`, `tbl_message`, `tbl_message_room`, `tbl_alarm`, `tbl_notification`. 스키마만 `exec/Dump20240403.sql`(MySQL 8.0.32 덤프, 데이터 없음). Spring Batch 메타 테이블은 덤프에 없음(`initialize-schema: never`).
- **Elasticsearch**: `police_acquired_data`, `police_matching_log`, `findear_matching_log`
- **Redis**: refresh token
- **S3**: 이미지
- **파일**: `colorDict.pickle`(호스트 볼륨), `model.bin`(이미지에 포함), `police_portal_list.csv`(레포)

## 7. 협업 도구 (당시)

- 원 저장소: SSAFY GitLab `lab.ssafy.com/s10-bigdata-recom-sub2/S10P22A706` (브랜치 develop / main-dev / batch-dev / match-dev / front-dev), MR 템플릿 `.gitlab/`
- Jira 키 `S10P22A706` — `infra/git-settings`의 git 훅이 브랜치명에서 이슈번호를 커밋에 자동 삽입
- Notion 요구사항·API 명세서 (README 링크)
- GitHub 계보: `EhighG/Findear`는 `2TF4/findear`의 **포크(public)**. README의 포팅 매뉴얼 링크는 `yee950419/findear` 미러를 가리킴

## 8. 유실된 정보 (레포에 없음)

| 항목 | 원래 위치 | 남은 단서 / 대안 | 문의 대상(README 역할·커밋 기준) |
|---|---|---|---|
| Config repo (main·batch yml + 비밀값) | SSAFY GitLab | 포팅 매뉴얼에 yml 골격만 있음(비밀값 placeholder) → 이번 복구에서 Config Server 자체를 제거 | 이상학 |
| Jenkins Job·Pipeline, Credentials, Webhook | EC2 `/var/jenkins_home` | 없음 → GitHub Actions로 대체 | 김동건 |
| 인프라 private repo 최신본 | 김동건님 개인 repo | 2024-04-04 스냅샷만 `infra/findear-infra-setting`에 복사됨 | 김동건 |
| fastText `model.bin` | 서버 | 불필요 (match mock) | – |
| Firebase 서비스계정 키, VAPID 키 | `key/`, front `.env` | 새 Firebase 프로젝트로 재발급 | – |
| front `.env` 실제 값 | 로컬 | 팀 종료 커밋에서 값을 비워서 올림 | – |
| EC2, 도메인, 인증서, S3 버킷 | SSAFY / AWS | 사용 불가 → 배포 시 새로 확보 | – |
| 운영 데이터 (MariaDB, ES) | EC2 | 없음 → Lost112는 API로 재수집, 나머지는 시드 | – |
| 개인 리팩토링 때의 `application-secret.yml` | 로컬 | 새 `.env` 체계로 대체 | 본인 |
| 개인 리팩토링 때의 DB 스키마(`findear`, `findear_batchdb`) | 로컬 | master에 DDL 없음 → Flyway로 새로 작성([06](06-db-and-config.md)) | 본인 |
| 아키텍처·ERD 원본 | README의 GitHub asset 이미지 | 이미지 링크만 있음 | 이상학 |
| Notion 명세서, Jira | 외부 | 접근 여부 미확인 | – |

## 9. 문서 ↔ 실제 설정 불일치 (참고)

- ES 버전: 포팅 매뉴얼 8.12.2 ↔ Dockerfile 7.17.3 (batch가 Boot 2.7 + `RestHighLevelClient`라 실제로는 7.x)
- **MongoDB**: 포팅 매뉴얼 컨테이너 목록에만 있음. README 배지는 2024-04-04에 추가됐다가 같은 날 삭제. 코드·의존성·compose·설정 어디에도 없음 → **한 번도 쓰인 적 없음**
- 포트: 매뉴얼 3100(프론트), 3240(config) ↔ compose는 둘 다 미게시
- Django: `requirements.txt` 5.0.3 ↔ pyproject(실제 빌드) 4.2.11
- 볼륨: MariaDB 볼륨이 데이터 경로가 아닌 `/backup`에, ES 볼륨은 경로 오타(`elasticearch`) → 데이터가 명명 볼륨에 영속화되지 않았음
- `redis/Dockerfile`(latest)은 미사용, compose는 `redis:7.2.4` 직접 사용
- `prev_conf.sh`는 다른 프로젝트(DB `comeet`) 스크립트
- `networks: my-network` 선언만 되고 미사용

## 10. 보안 이슈 (값은 적지 않음)

git에 평문으로 남아 있는 비밀값 — **public 레포이며 원본 `2TF4/findear`에도 같은 히스토리가 공개돼 있음**. 결정: 키는 폐기(rotate)만 하고 히스토리 재작성은 하지 않음([D-15](03-decisions.md)). 이번 복구는 전부 새 키를 사용.

| 비밀값 | 위치 | 비고 |
|---|---|---|
| OpenAI API 키 | `infra/findear-infra-setting/match/.env` (master HEAD에도 존재) | R-02에서 HEAD에서 삭제 |
| SSAFY GitLab 계정 비밀번호 | `infra/findear-infra-setting/config/application.yml` + 같이 커밋된 config jar | 〃 |
| TLS 개인키 (만료) | `infra/findear-infra-setting/web/cert/privkey.pem` | 〃 |
| Firebase Admin 서비스계정 JSON 2개 | 히스토리 (`d24dad8`, `1506901`에서 삭제) | 프로젝트 `findear-bfd63` |
| VWorld 키 | main `LocationController` 하드코딩 (master에도) | R-21에서 환경변수로 이동 |
| Kakao JS 키 | `front/index.html` | JS 키는 공개 전제지만 도메인 제한 필요. 프론트 복구 시 교체 |
| Naver Client ID/Secret | `Chore/10-reset_env` 브랜치 `76edc42`의 `front/.env` (**사용자 본인 키**) | **재발급 필요 (U-01)**. 브랜치를 지워도 커밋 해시로 조회 가능 |
| Django `SECRET_KEY` | `match/findear/settings.py` | match는 mock으로 대체 |
| 기본 계정 | Config Server `findear/findear`, DB `findear/findear`·root `root` | – |

구조적 문제(재구성 시 반영):
- S3 IAM 키를 프론트 번들에 포함 → 서버가 presigned URL 발급([D-13](03-decisions.md))
- Naver `client_secret`을 프론트 URL에 포함 → 서버에서만 사용
- Docker 게시 포트의 UFW 우회 → DB/ES/Redis는 `127.0.0.1` 바인딩 또는 미게시
- Config Server 외부 노출 → Config Server 제거
