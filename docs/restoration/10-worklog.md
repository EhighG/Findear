# 10. 작업 로그

> 세션이 끝날 때마다 맨 위에 추가하세요. 형식: 날짜 / 세션(환경·브랜치) / 한 일 / 남은 일·주의사항.

## 2026-10-09 (2) — 로컬 Claude Code, Windows 11 (작업 방식 전환: ADR·PR, mattpocock-skills 설정)

**한 일** — subagent 없이 메인이 직접 (사용자와 대화)
- `/mattpocock-skills:setup-matt-pocock-skills` 실행: `docs/agents/`(issue-tracker: GitHub + 이 레포 규칙, triage-labels: 기본 5종, domain: single-context + 이 레포 규칙), CLAUDE.md "Agent skills" 블록. 라벨 4종(`needs-triage` 등)은 아직 GitHub에 없음 — `triage`를 처음 쓸 때 생성.
- 결정 D-65(사용자): 결정 기록은 `docs/adr/`, 용어는 루트 `CONTEXT.md`, 03은 D-65까지로 동결(기존 D-xx는 옮기지 않음).
- ADR-0001(사용자): 이후 master 반영은 PR + squash merge — D-34의 "PR 없음"·fast-forward 병합과 D-39(`add-issue-ref.sh`)를 대체. 03 D-34·D-39 행에 링크, CLAUDE.md·08 진행 절차·루트 README 리팩토링 규칙·이 폴더 README §6 갱신.
- 08 U-10: 새 토큰에 Pull requests·Contents 쓰기 권한 추가 필요로 갱신.

**주의**
- 현재 gh 토큰(Issues·Actions)으로는 PR 생성·병합이 막힐 수 있다 — U-10 갱신 전이면 PR 생성·병합은 사용자가 웹에서 하거나 권한을 추가.

## 2026-10-05 ~ 10-09 — 로컬 Claude Code, Windows 11 (`fix/12-r91-results`, R-91 완료)

**한 일 (R-91, 상위 이슈 #12)** — subagent 없이 메인이 직접 (사용자와 대화하며 키 세팅을 함께 진행)
- 사용자 키 세팅 지원: U-01(Naver Secret 재발급, 값은 `.env`에만 — Naver 로그인은 추후 D-50), U-04(Firebase 새 프로젝트), U-05(data.go.kr 2종 활용신청), U-07(VWorld 지오코더·검색 API, 서비스 URL `http://localhost`) 완료. 콘솔의 npm 예시 코드에서 `firebaseConfig`만 옮겨 `tools/fcm-test/firebase-config.js` 작성(`measurementId`·Analytics는 불필요, 테스트 페이지는 CDN이라 `npm install` 불필요). 사용자가 vapidKey를 추적 파일인 `firebase-config.example.js`에 넣은 것을 git 제외 파일로 옮기고 예시는 되돌림.
- 로컬 `.env`가 예전 `.env.example`로 만들어져 `LOST112_*`·`BATCH_SCHEDULING_ENABLED`가 없던 것을 덧붙임(compose 기본값이 있는 나머지 `MATCH_*`·`IMAGE_*` 등은 그대로).
- 결정 D-63: R-91 확인 요청은 사용자 요청으로 Claude가 실행(D-38 예외, R-91 한정). `docker compose up -d --build main batch` → `verify.sh --yes`: VWorld OK, Lost112 수집 성공(경찰청 51페이지·50,742건, 포털기관 43페이지·42,052건 → ES 89,528), FCM 브라우저 알림 수신(사용자) + `--fcm-phone` 200. 결과는 08 R-91.
- 수정: `verify.sh` 테스트 발송 본문의 한글이 Git Bash curl 인자에서 ANSI로 바뀌어 400 → stdin(`--data-binary @-`)으로. Lost112 트래픽 문구를 "10,000건"에서 호출 수로 정정(data.go.kr 상세 페이지 오류 문구 "일일 호출 허용량", 실제로 42,052건 받아도 한도 안) — `.env.example`·`verify.sh`·`tools/verify-external/README.md`·05·03 O-4.
- 08: U-01·U-04·U-05·U-07·R-91 체크. 사용자 확인: U-02 Secret scanning·Push protection 켜짐(체크) / U-08 배포는 당분간 안 함 / U-10은 만료일 즈음 사용자가 처리.
- 결정 D-64(O-4 해결): Lost112 정기 수집은 끄고 필요할 때 수동 수집(사용자가 서버를 상시 띄우지 않음). `.env.example` 머리말·수집 주석 갱신.
- 로컬 `.env`를 현재 `.env.example` 구조로 다시 만듦(기존 값 53개 그대로, 빠졌던 `MATCH_*`·`BATCH_MEM_LIMIT`·`IMAGE_*`·`ELASTIC_PASSWORD`는 compose 기본값과 같은 예시 값 → 동작 변화 없음, 사용자 주석인 VWorld 만료일 유지).

**주의**
- Claude Code의 도구 입력에서 `\uXXXX`가 실제 문자로 바뀌어 기록된다(Edit·Bash 모두) — 파일에 JSON `\u` 이스케이프를 쓰려면 다른 방법을 쓴다.
- `LOST112_COLLECT_ENABLED`는 false 유지(정기 수집은 사용자 결정 대기). 키가 있으면 수동 `POST /search/save`는 동작.

- 상위 이슈 #12 "Findear 복구 1차" 닫음(Phase 이슈 #13~#21 모두 닫힌 것 확인).

**다음 세션**
- 사용자가 고른 "1차 목표 이후" 작업부터, 새 이슈로. 사용자: U-10(2026-10-17).

## 2026-10-01 (6) — 로컬 Claude Code, Windows 11 (`test/21-final-verification`, Phase 8 완료 — Claude의 1차 작업 끝)

**한 일 (Phase 8, 이슈 #21 / 상위 #12)** — 사용자가 자리를 비우며 이어서 진행하라고 해서 Phase 7 보고 뒤 바로 착수. D-46 방식
- R-90: executor가 master `897a86f`를 GitHub에서 scratch로 깨끗하게 clone(프로젝트 `findear`), `.env`는 호스트 포트 3개만 바꿔 **모니터링까지 15개 서비스 전체 기동**(이 단계에서만 허용, D-32) → 08 R-90 시나리오 1~11 수행·기록, 스택은 띄운 채 보고. verifier가 떠 있는 스택을 직접 조회해 1~11 재판정(PASS)하고 기동 27분 시점에 자원 재측정. 메인이 `down -v`.
- 결과 요약: 전부 통과. 기동 약 73초(빌드 캐시), Prometheus 9/9 up, 패널 167 data / 23 정상 없음 / error 0, 시계열 약 11,000(버킷 약 4,000), OOMKilled·재시작 0, 사용 합계 약 3,410MiB / 제한 4,000MiB. 상세는 08 R-90 결과, 04 §5 "R-90 실측" 열.
- 결정 D-62: 메모리 기본값은 최소 그대로(OOM 0 — DoD·04 §5 규칙), 최대 90% 이상인 ES 97%·MySQL 98%·cAdvisor 97%·Prometheus 94%·main 90%는 오래 켜 두는 환경·배포에서 "여유" 값 권장(합계 약 5.3GB). O-2(배포 EC2 사양) 문구도 갱신.
- 발견 → 문서 반영(코드 변경 없음): ① R-50부터 남아 있던 "등록 API 약 2.1초"는 **Windows 클라이언트의 `localhost` 지연**(포트는 `127.0.0.1`에만 게시, `::1`을 먼저 시도해 약 2초 재시도. 서버 처리는 0.04~0.1초, curl·브라우저는 Happy Eyeballs로 약 0.2초) → CLAUDE.md 빌드 메모 ② `GET /matchings/*/total`의 `lostBoardId`는 분실물 id(게시글 id면 batch 404 → main 500, 기존 동작) → 07 §4 ③ cAdvisor 대시보드 `container` 변수에 호스트의 다른 컨테이너가 섞임 → 대시보드 README.
- 이슈 #21 닫음, 상위 #12의 Phase 8 체크.

**주의**
- executor 결과 기록의 cgroup anon 값은 바이트/1e6을 MiB로 적은 단위 오류가 있었다(verifier가 발견) — 문서에는 정정값(ES anon 93%, MySQL 94%)을 썼다. working set 값은 맞음.
- Prometheus 3.x는 기본으로 `GOMEMLIMIT`를 제한×0.9로 잡아 working set이 90% 근처에 머문다(정상, 튜닝 아님).
- FCM 비활성 로그(`FCM 비활성: 발송 건너뜀`)는 토큰이 저장된 회원에게만 찍힌다(없으면 debug). R-90은 더미 토큰을 DB에만 저장해 확인.
- `docker compose up -d batch`는 의존 관계 때문에 flyway one-shot도 다시 실행한다(exit 0, 변경 없음).
- clone·결과(`results.md`, 패널·자원 원시 출력)는 이 세션 scratchpad `r90/`, verifier 산출물은 `r90-verify/`(레포 밖).

**다음 세션**
- Claude의 1차 작업 끝. 사용자: U-10(gh 토큰 2026-10-17 만료), R-91(키 세팅 → `verify.sh`·`tools/fcm-test`, 05 §9), U-01·U-02, 배포 시 U-08. 이후 작업은 08 "1차 목표 이후"에서 사용자가 고른 것부터.

## 2026-10-01 (5) — 로컬 Claude Code, Windows 11 (`feature/20-*`, Phase 7 완료)

**한 일 (Phase 7, 이슈 #20 / 상위 #12)** — D-46 방식(지시서 → executor → verifier → 메인이 커밋·master 반영)
- R-80 (`feature/20-fcm-test-page`): `tools/fcm-test/` — `index.html`·`app.js`(ES 모듈), compat 서비스 워커, `sdk-version.js`(JS SDK 12.19.0, 페이지·서비스 워커 공용), `firebase-config.example.js`(`self.FINDEAR_FCM_CONFIG`), README. 상태 계약 `body[data-fcm-state]`로 headless Chrome 검증: 외부 이름 해석을 막고(`--host-resolver-rules`) net log로 설정 없음·빈 값·가짜 값 세 경우에 페이지발 외부 연결 0. main 부분 기동으로 origin `http://localhost:5500`의 preflight·로그인·토큰 저장·테스트 발송(FCM 비활성) 계약 확인. 검증 FAIL(문서): client 문서가 `getToken`을 deprecated로 두고 FID(`register`/`onRegistered`)를 권장한다는 사실이 05 §8에 빠짐 → 메인이 기록하고 **D-61**(1차는 등록 토큰 유지, FID 전환은 1차 이후 — 08 "1차 목표 이후").
- R-81 (`feature/20-verify-external`): `tools/verify-external/verify.sh`(설정 검사 → 설정된 연동만 main·batch로 확인 요청 → 요약, 키가 없으면 HTTP 요청 0)·README, 05 §9 키 세팅 체크리스트 완성. 가짜 `curl`로 시나리오 시험. 검증 FAIL: 한글 쿼리를 셸에서 바이트 단위로 인코딩하는 함수가 `bash:3.2`·musl `bash:5` 이미지에서 틀린 URL을 만듦 → 메인이 미리 인코딩한 상수로 바꾸고 세 셸에서 같은 URL 확인, README 깨진 링크 수정. 08 R-90 9번을 "verify.sh는 키가 없으면 요청하지 않음 → 503은 직접 요청해 확인"으로 정리.
- 이슈 #20 닫음, 상위 #12의 Phase 7 체크.

**주의**
- 이 PC 로컬 `.env`에 `LOST112_*` 변수가 없다(예전 `.env`). 기본값으로 동작하지만 R-90 1단계에서 `.env.example`과 대조할 것.
- headless Chrome net log에는 Chrome 자체 백그라운드 요청(update·accounts·autofill 등)이 보인다 — `--host-resolver-rules`로 모두 이름 해석 단계에서 실패하고 페이지 요청과는 무관.
- Git Bash에서 curl로 한글 JSON 본문을 인자로 보내면 ANSI로 가 main이 400 → UTF-8 파일(`--data-binary @file`)로 보낸다.
- R-80 검증으로 `findear` 볼륨을 `down -v`로 지웠다(로컬 개발 데이터 없음 — R-90은 깨끗한 상태에서 시작).
- `verify.sh`는 Windows에서 `git add --chmod=+x`로 100755 커밋(`core.filemode=false`).

**다음 세션**
- R-90(Phase 8 최종 검증, 08의 1~11 — 이 단계에서만 모니터링까지 전체 기동·자원 실측). 사용자: U-10(gh 토큰 2026-10-17 만료).

## 2026-10-01 (4) — 로컬 Claude Code, Windows 11 (`feature/19-*`, Phase 6 완료)

**한 일 (Phase 6, 이슈 #19 / 상위 #12)** — D-46 방식(지시서 → executor → verifier → 메인이 커밋·master 반영). R-63 수정과 R-64는 git worktree로 병렬 진행(docker·파일이 겹치지 않게)
- R-60 (`feature/19-ci`): `ci.yml` — 모듈 matrix 빌드·테스트, 문서만 바뀐 커밋 건너뜀, 실패 테스트 예외 전체를 로그로(CI 전용 init 스크립트). GitHub 첫 실행 성공(main 약 3분·batch 약 4분·match 41초, Testcontainers 동작). 검증에서 `setup-gradle` v6 기본 캐시가 상용 구성요소라 약관 동의가 따르는 것을 발견 → `cache-provider: basic`(D-59).
- R-61 (`feature/19-ghcr-images`): `images.yml`(수동·master만, `latest`·전체 SHA, platforms 입력), 로컬 override 앱 서비스 `pull_policy: build` — `docker compose pull`은 건너뛰고 `up`마다 다시 빌드·재생성(04 §4에 기록).
- R-62 (`feature/19-compose-prod`, 지시서 두 단계): 앱 쪽 — main·batch `ProfileGuardConfig`(local+prod면 기동 실패, D-60), batch ES 인증 속성. 오버레이 — prod 프로필 고정, AWS S3만(seaweedfs 끔, `depends_on: !override`), main만 80, Redis·ES 인증(앱·exporter), 모니터링 127.0.0.1, node-exporter, Prometheus `scrape.d/{local,prod}`, Grafana `host/`(1860), restart·로그 로테이션. prod 오버레이를 로컬에서 프로젝트 `findear-prodtest`로 두 묶음 부분 기동해 확인(인증 거부/성공, 프로필 prod, 80 응답, ES 조회 API).
- R-63 (`feature/19-deploy-scripts`): `init-host.sh`, `deploy.sh`(`.env` 검사·`--check`·`--tag`·pull 뒤 재실행). 검증 FAIL 2회 — 1차는 지시서가 `secrets/`를 700으로 정해 컨테이너 uid 10001이 FCM 파일을 못 읽는 문제(메인 지시서 오류) 등, 2차는 안내의 `chgrp 10001`을 배포 사용자가 sudo 없이 못 하는 문제 → 메인이 setgid 2750·`sudo chgrp` 안내·`exec bash`로 고치고 alpine으로 직접 확인. `vm.max_map_count`는 Elastic 문서 현재 값 1048576(계획의 262144에서 바꿈).
- R-64 (`feature/19-aws-kit`, worktree): `infra/aws/` — setup-s3.sh(Public Access Block + storage-init.sh 재사용, SeaweedFS 리허설), IAM 정책 템플릿·신뢰 정책·setup-iam.sh(`--render-only`), README. 검증 권고로 IMDS hop limit 2(컨테이너 자격증명)·실행 환경·깨진 앵커를 메인이 반영.
- R-65 (`feature/19-deploy-workflow`): `deploy.yml`(수동·master, 입력·시크릿 env로만, 호스트 키 사전 확인). 검증 권고로 `environment: production`·ssh 대상 형식 검사를 메인이 반영.
- 결정: D-59(CI Gradle 캐시 basic), D-60(prod 프로필 고정·local+prod 가드). 이슈 #19 닫음, 상위 #12의 Phase 6 체크.

**주의**
- 실제 배포·AWS·EC2는 하지 않았다(D-41). 서버에서 확인할 것은 08 R-62~R-65 메모: node-exporter(Docker Desktop은 `rslave` 마운트 불가로 기동 못 함)·1860·7362 node 패널, IMDS hop limit 2, 계정 수준 Block Public Access, 첫 `up --wait` 시간, GHCR 패키지 public.
- 이제 push마다 CI가 돈다(같은 ref는 이전 실행 취소). 작업 브랜치 push → CI 확인 → master 반영 순서로 했다.
- 이 PC: 호스트 3000·9090·3001을 다른 프로젝트가 쓸 때가 있다(prod 오버레이 시험은 9091·3011로). 기본 findear 프로젝트 볼륨 `findear_mysql-data`·`findear_seaweedfs-data`가 이번 실험으로 생겨 남아 있다(로컬 개발용, R-90 전에 `down -v`로 비워도 됨).
- Git Bash에서 Python heredoc에 Windows 경로(역슬래시)를 넣으면 유니코드 이스케이프 오류가 난다 → 그런 문서 수정은 Edit 도구로.

**다음 세션**
- Phase 7(R-80 → R-81) → R-90. 사용자: U-10(gh 토큰 2026-10-17 만료).

## 2026-10-01 (3) — 로컬 Claude Code, Windows 11 (`feature/12-phase6-direction`, 문서만)

**한 일 (Phase 6 착수 전 방향 결정, 코드 작업 없음)**
- 사용자와 Phase 6 범위 확인: 1차 목표는 로컬 docker compose 실행, 실제 배포(AWS·EC2)는 나중 — Phase 6은 배포 준비물(워크플로, `compose.prod.yml`, 스크립트, AWS 키트)을 만들고 로컬 검사(config·문법·SeaweedFS)까지(P5, D-41).
- GHCR 이미지를 미리 올리는 것의 우려점 검토(비밀값 노출 없음 — 깨끗한 checkout으로 빌드, 오래된 공개 이미지 누적, 로컬 빌드 이미지와 이름이 같아 `docker compose pull` 시 섞일 수 있음) → **D-58**: CI(`ci.yml`)는 자동·테스트만, `images.yml`은 `workflow_dispatch`로만, 로컬 override 앱 서비스는 `pull_policy: build`. 08 R-60·R-61, 09 §2 반영.
- U-03: 포크 레포 Actions는 이미 켜져 있음(사용자 확인) → 완료.
- gh 토큰 확인: OS 키링의 fine-grained PAT(사용자 이름 AI-based-dev, 만료 2026-10-17 11:58 UTC)이고 Actions 권한이 있음(`actions/runs` 200). 앞서 Actions 설정 조회 403을 "Actions 권한 없음"으로 잘못 설명했었음 — 그 API는 Administration: read가 필요(작업에는 불필요). 08 U-10·README의 "Actions 읽기 권한 추가" 문구 정정.

**다음 세션**
- Phase 6(R-60 → R-65)을 D-58대로. 이슈는 착수 때 `gh issue create --parent 12`. 사용자: U-10(10/17 전 갱신).

## 2026-10-01 (2) — 로컬 Claude Code, Windows 11 (`feature/18-*`, Phase 5 완료)

**한 일 (Phase 5, 이슈 #18 / 상위 #12)** — D-46 방식
- R-50 (`feature/18-app-metrics`): Prometheus job main·batch·match, main HTTP 클라이언트 시간 제한(`spring.http.client` 3s/10s, `spring.http.reactiveclient` 연결 3s — 상대가 멈췄을 때 실패 감지 30s·14s → 3s), 커스텀 지표 3종(`findear_lost112_ingest_runs_total`·`_items_total`, `findear_fcm_send_total`, 0으로 미리 등록), batch Spring Batch 5.2 지표 중복 WARN을 `MeterFilter`로 제거(spring-batch#4753, 첫 잡 실행 때 나던 것). 실행 결과에서 main→batch 호출의 `uri` 태그에 id·검색어가 들어가는 고카디널리티를 메인이 보고 batch 전용 RestTemplate + URI 템플릿으로 고치게 함(R-35 엄격 인코딩 유지).
- R-51 (`feature/18-grafana-dashboards`): Grafana 대시보드 provisioning, 가져온 대시보드 6개(4701·19004·7362·763·14191·14282), Findear Overview(행 8·패널 25), 앱 HTTP 히스토그램. 검증 FAIL 1건(메모리 비율 패널 `on (name)` → 컨테이너 재생성 직후 쿼리 오류)을 메인이 `on (id, name)`으로 고치고 재생성 직후 옛·새 쿼리를 직접 비교해 확인.
- 결정: D-57 Grafana 메모리 기본값 192m → 512m (대시보드를 열면 256m·384m에서도 OOM, 공식 최소 권장 512MB). 04 §5 합계 약 3.9GB, Docker Desktop 권장 최소 5GB.

**주의**
- 이 PC 로컬 `.env`의 `GRAFANA_MEM_LIMIT`를 512m로 바꿈(예전 `.env`를 쓰는 환경은 같이 바꿔야 함 — `.env.example`은 512m).
- 대시보드 패널 검사는 Grafana `/api/ds/query`로 모든 패널을 실행해 분류하는 방식(스크립트는 레포 밖 scratchpad). 확인용 트래픽은 15초 이상 간격으로(수집 간격 안에 몰리면 `rate`가 비어 보임).
- 하지 않은 것(기본값 유지): Tomcat mbeanregistry(JVM Utilisation 패널), es-exporter `--es.indices`(ES Indices 행).
- `POST /acquisitions`·`POST /losts` 응답이 약 2.1초(원인 미조사, R-90에서 확인).

**다음 세션**
- Phase 6(배포 준비, R-60 → R-65). R-60(CI)·R-62(`compose.prod.yml`)·R-63·R-64 메모 확인 — Testcontainers(Docker) 테스트, `local`/`prod` 프로필 가드, AWS S3 구성 분기, batch ES 인증, 배포 대시보드(Node Exporter Full). 사용자: U-03(Actions 활성화, R-60), U-10(gh 토큰 2026-10-17 만료 — Actions: Read 권한 포함 권장).

## 2026-10-01 — 로컬 Claude Code, Windows 11 (`feature/17-*`, `fix/17-*`, Phase 3 완료)

**한 일 (Phase 4 재점검 → Phase 3, 이슈 #17 / 상위 #12)** — D-46 방식(지시서 → executor → verifier → 메인이 커밋·master 반영)
- Phase 4 재점검: match 테스트 44건·main 203건을 새로 돌려 통과, match만 부분 기동해 healthy·픽스처 3종 200·관리 포트 비공개·OOM 없음 확인. 이슈 #16 닫힘·#12 체크 확인. 문서 불일치는 08의 R-21 체크박스(완료인데 `[ ]`) 하나 → 수정.
- R-30·R-31 (`feature/17-batch-boot35`, 한 브랜치): 팀 batch를 Boot 3.5.16으로(jakarta, Batch 5, `RestHighLevelClient` → `ElasticsearchOperations`, Spring Cloud 제거, Dockerfile), 설정 외부화·compose `batch`·`BATCH_HOST_PORT`. 레포에 설정 파일이 없어(Config Server) 기동 확인과 설정 외부화를 함께. executor의 파일 삭제(`alarm/` 패키지, ES 7 설정)가 자동 모드 분류기에 막힘 → 사용자에게 이유를 설명하고 승인받아 메인이 `git rm`.
- R-32 (`feature/17-lost112-collect`): 공공데이터포털 상세 페이지로 Lost112 두 서비스 명세 확인(05 §8, API 호출 없음) → 수집 재작성(페이지 단위 bulk upsert, 문서 ID atcId, 키 한 번 인코딩, XXE 방지, 503/502, D-53), 샘플 적재 `infra/elasticsearch/seed/`. e2e는 WireMock + relaxed binding `LOST112_BASEURL`.
- R-33 (`feature/17-matching-log-id`): 매칭 로그 매핑·결정적 ID·분실물 단위 교체(D-54). executor가 "유효 0건이면 전부 삭제"로 구현한 것을 메인 판단으로 "유지"로 보완.
- R-34 (`feature/17-batch-jobs`): findearJob(습득물 PK를 보내던 버그, 조기 return, 삭제 필터) 수정, policeJob 수집 on/off + Lost112 매칭 구현, 분실물 단위 실패 격리, 수동 실행 API(D-55). e2e에서 두 잡 동시 시작 교착 발견 → 재시도 대신 잡 생성 격리 수준 `read_committed`(검증 지적 반영).
- R-35 (`fix/17-main-batch-contract`): main 분실물 등록 직후 batch 매칭 요청을 커밋 후로(R-41 방식), 알림 규칙, Lost112 목록 쿼리 엄격 인코딩. main·batch·match 전체 흐름 e2e.
- R-36 (`feature/17-batch-cleanup`): 테스트 엔드포인트 삭제, 전체 조회·삭제는 `@Profile("local")` 컨트롤러, 공통 오류 응답(`{status, message}`, 404·400·502·500, D-56), 예외 래핑·`System.out`·`printStackTrace` 정리, 쓰지 않는 클래스 5개 삭제(보고 → 메인이 `git rm`). batch 테스트 143개, main 235개.
- 결정: D-53(Lost112 수집), D-54(매칭 로그 교체), D-55(잡 실패 격리), D-56(batch 오류 응답·local 한정), D-52 적용 범위 확장. 이슈 #17 닫음, 상위 #12의 Phase 3 체크.

**주의**
- 자동 모드 분류기가 subagent의 파일 삭제(`rm`)를 막는다 → 지시서에 "삭제 필요는 보고만" 규칙을 넣고 메인이 `git rm`(git 추적 파일만).
- 이 PC `.env`: `BATCH_HOST_PORT=8092` 추가. `COMPOSE_PROFILES=monitoring`이므로 부분 기동은 서비스 이름을 지정(또는 셸에서 `COMPOSE_PROFILES=`).
- batch 테스트(124+)는 Testcontainers MySQL·ES를 써서 Docker 필요, 약 1분 30초.
- Lost112 실제 응답 구조·트래픽은 R-91에서 확인할 것이 많음(05 §8).
- 시드 습득물은 `registered_at`이 오늘-2일이라 분실일이 그보다 늦은 분실물과는 Findear 매칭이 안 됨(의도된 후보 조건).

**다음 세션**
- 사용자에게 Phase 3 보고 후 Phase 5(모니터링 연결, R-50 → R-51). R-50 메모(08) 확인: batch 잡 지표·Micrometer 태그 충돌, 수집 실패는 스텝 지표로, WebClient 연결 시간 제한, 공용 RestTemplate 타임아웃.
- 사용자: U-10(gh 토큰 2026-10-17 만료).

## 2026-09-30 (8) — 로컬 Claude Code, Windows 11 (`feature/16-match-mock`, `fix/16-autofill-after-commit`, Phase 4 완료)

**한 일 (Phase 4, 이슈 #16 / 상위 #12)** — D-46 방식(지시서 → executor → verifier → 메인이 커밋·master 반영)
- R-40 (`feature/16-match-mock`): `match/` Spring Boot 3.5.16 mock 앱. 팀 시절 Django 코드(`2af1413:match/`)와 main·batch DTO에서 요청·응답 모양을 확인해 경로·JSON을 맞춤. `/process`는 SHA-256 결정적 선택·키워드 항상 5개, 매칭은 `MatchingScorer`(교체 지점, O-1) + 결정적 기본 점수, 오류 응답 `{"message"}`, compose `match`(256m)·override(`MATCH_HOST_PORT`)·`.env.example`. 테스트 44개(기대 점수는 테스트에서 명세 공식으로 따로 계산), 검증에서 응답값을 Python으로 따로 계산해 일치. 기본 scorer를 일반 `@Configuration`에 두면 사용자 빈과 두 개가 되는 것을 실행 중 테스트로 발견 → `@AutoConfiguration`으로 등록.
- R-41 (`fix/16-autofill-after-commit`, R-40 지시서를 쓰다 발견): main 습득물 등록이 트랜잭션 커밋 전에 match `/process`를 비동기 호출하고, 콜백이 다른 스레드에서 등록 시점 엔티티를 merge → 커밋 후 이벤트로 요청, 새 트랜잭션에서 빈 컬럼만 채움(D-52), Builder 빈·30s 타임아웃. main 테스트 203건, e2e로 mock 값 반영·지연 중 수정 보존·match 중지 시 WARN 한 줄 확인.
- 문서: 07 §5(구현 세부·main 쪽 동작), 06(`MATCH_HOST_PORT`, `autofill-timeout`), 03(D-52, O-1), 08(R-31·R-35·R-50 메모). 이슈 #16 닫음, 상위 #12의 Phase 4 체크.

**주의**
- match 컨테이너를 멈추면 main의 자동채움 실패 WARN이 약 30초 뒤에 찍힘 (Docker 내장 DNS가 없는 이름 조회에 약 8초씩 걸림). 동작에는 문제 없음 → R-50에서 연결·해석 시간 제한 검토.
- main e2e의 인증 헤더는 `Authorization: Bearer`가 아니라 **`access-token: <토큰>`** (`JwtFilter`).
- 이 PC는 호스트 8082를 다른 프로젝트(`qqueueing-*`)가 씀 → Phase 3에서 batch 게시 포트 변수화(08 R-31 메모).
- executor 보고: 이 환경의 Bash heredoc이 역슬래시를 바꿀 수 있어 역슬래시가 든 소스는 Write/Edit 도구로 작성.

**다음 세션**
- Phase 3(batch 복구, R-30 → R-36). 착수 시 이슈를 `gh issue create --parent 12`로 생성하고 08의 R-30·R-31·R-35 메모부터 확인. 사용자: U-10(gh 토큰 2026-10-17 만료).

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
