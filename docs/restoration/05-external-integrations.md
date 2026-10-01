# 05. 외부 연동

> 원칙: 기능상 필요한 **무료 연동은 전부 복구**, 유료는 사용자 보고 후 판단 (P4).
> 발급받은 값은 `.env` 또는 `secrets/`에만 둡니다. **절대 커밋 금지** (public 레포).
> 환경변수 이름은 [06-db-and-config.md](06-db-and-config.md#6-환경변수-전체-목록)와 일치해야 합니다.
>
> **1차 작업의 진행 방식 (D-38)**: 외부 키는 1차 작업이 끝난 뒤 사용자가 발급·세팅합니다. 그래서 작업·검증 중에는 이 문서의 외부 API(Firebase, Lost112, Naver, VWorld, AWS)를 **호출하지 않습니다** (키 없이 보내는 요청 포함). 대신
> 1. 각 연동을 **공식 문서의 현재 명세 기준**으로 구현하고, 확인한 문서를 §8에 기록합니다.
> 2. 외부 API 주소는 설정값(기본값은 공식 주소)으로 두고, 테스트에서는 로컬 mock 서버에 공식 문서의 요청 형식·응답 예시(오류 포함)를 재현해 검증합니다.
> 3. 키가 없어도 앱은 기동하고, 해당 기능만 "설정 필요" 오류로 응답합니다.
> 4. §9 체크리스트대로 키를 채우면 **코드 수정 없이 바로 동작**하는 것이 목표입니다. 실제 동작 확인은 사용자가 R-91에서 합니다.
> 5. AWS는 실제 연결이 필요한 검증을 하지 않습니다 (D-41). 로컬 SeaweedFS로 같은 동작을 볼 수 있는 부분(버킷·CORS 명령, presigned URL)만 확인하고, AWS 전용 부분(IAM, 버킷 정책, EC2)은 문법 검사까지 합니다.

## 1. 요약

| 연동 | 사용처 | 1차 복구 | 비용 | 환경변수 / 파일 | 발급 작업 |
|---|---|---|---|---|---|
| Firebase Cloud Messaging | main(발송), `tools/fcm-test`(토큰 발급) | O | 무료 (Spark) | `FCM_ENABLED`, `FCM_CREDENTIALS_PATH`, `secrets/firebase-adminsdk.json`, `tools/fcm-test/firebase-config.js` | U-04 |
| 공공데이터포털 Lost112 API 2종 | batch | O | 무료 | `LOST112_SERVICE_KEY` | U-05 |
| Naver 로그인 | main | O | 무료 | `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`, `NAVER_REDIRECT_URI` | U-06 |
| VWorld (공간정보 오픈플랫폼) | main (`/location/*` 프록시) | O | 무료 | `VWORLD_API_KEY` | U-07 |
| S3 호환 스토리지 | main (presigned URL) | 로컬은 SeaweedFS | 로컬 무료 / **AWS S3는 유료** | `STORAGE_*`, `AWS_*` | 배포 시 (U-08) |
| GitHub Actions / GHCR | CI, 이미지 | O | 무료 (public 레포) | – | U-03 |
| Kakao Maps JS SDK, Daum 우편번호 | front | X (프론트 복구 때) | 무료 | – | 프론트 복구 시 |
| OpenAI (GPT-4 Vision) | (match) | **제외** (P1) | 유료 | – | – |
| KATS 한국색채 크롤링, fastText | (match) | **제외** (P1) | – | – | – |
| 소상공인 상가정보 API | (front, 과거) | **제외** (2024-03 이후 미사용) | – | – | – |
| 이메일 (spring mail) | – | **제외** (미사용, 의존성 제거) | – | – | – |

## 2. Firebase Cloud Messaging (웹푸시)

**현재 코드**: main `Alarm/service/FCMInitializer`(초기화 비활성, 경로 하드코딩), `NotificationService`(WebpushConfig로 단건 발송 `sendAsync`), 토큰 저장 `POST /notification/new` → `tbl_notification`. 발송 시점: 분실물 등록 후 첫 매칭 결과, 쪽지 전송·답장, 테스트 `POST /alarm/send-fcm/{memberId}`.

**발급 (U-04)**
1. Firebase 콘솔에서 새 프로젝트 생성 (Spark 요금제, 결제 수단 불필요). 옛 프로젝트 `findear-bfd63`은 사용하지 않음.
2. 프로젝트 설정 → 서비스 계정 → "새 비공개 키 생성" → JSON을 `secrets/firebase-adminsdk.json`으로 저장.
3. 프로젝트 설정 → 일반 → 웹 앱 추가 → `firebaseConfig` 값을 `tools/fcm-test/firebase-config.js`(git 제외)에 저장.
4. 프로젝트 설정 → 클라우드 메시징 → 웹 푸시 인증서 → 키 쌍 생성 → VAPID 공개키를 테스트 페이지 설정에 저장.
5. `.env`에서 `FCM_ENABLED=true`.

**코드 작업 (R-23)**: `firebase-admin` 7.1.1 → 9.x, 초기화를 `fcm.enabled` 조건부로 (키 없이도 앱 기동), 자격증명 경로는 `FCM_CREDENTIALS_PATH`, `main/.gitignore`의 `key/` 예외 제거(K-08).

**구현 (R-23, 2026-09-30)**: `Alarm/push/` — `PushSender` 인터페이스와 두 구현(`FcmPushSender`, `NoopPushSender`)을 `fcm.enabled`로 고른다(`@ConditionalOnFcm`: Spring의 boolean 변환이라 `true/false/yes/no/on/off/1/0`, 비어 있거나 없으면 false, 그 밖의 값이면 기동 실패). `true`면 `FcmConfig`가 `FCM_CREDENTIALS_PATH` 파일로 `FirebaseApp`·`FirebaseMessaging` 빈을 만들고, 파일이 없거나 읽을 수 없으면 원인을 적은 메시지로 기동을 멈춘다. `NotificationService.sendNotification`은 알림(`tbl_alarm`)을 저장하고 `PushRequestedEvent`를 발행하며, 푸시는 **트랜잭션 커밋 후**(`@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)`, 트랜잭션 없는 호출은 즉시) 발송된다 — 발송 실패는 호출한 흐름(쪽지·분실물 등록)을 깨지 않고, `UNREGISTERED`면 저장된 토큰을 별도 트랜잭션(`REQUIRES_NEW`)으로 삭제한다. 메시지는 웹푸시 `notification`(title, body = `메시지:타입`)과 `token`. compose는 `./secrets`를 `/run/secrets`에 읽기 전용 디렉토리로 마운트 — 배포(Linux)에서는 파일을 컨테이너 사용자(uid 10001)가 읽을 수 있게(예: `chmod 644`).

**구현 (R-80, 2026-10-01)**: `tools/fcm-test/` 정적 페이지(프레임워크·빌드 없음, 실행·단계는 [README](../../tools/fcm-test/README.md)). 파일: `index.html` + `app.js`(ES 모듈), `firebase-messaging-sw.js`(서비스 워커), `sdk-version.js`(Firebase JS SDK 버전의 유일한 정의, 현재 **12.19.0** — 페이지와 서비스 워커가 함께 읽음), `firebase-config.example.js`(복사해 `firebase-config.js`로 채움, git 제외). 설정 형식은 `self.FINDEAR_FCM_CONFIG = { firebaseConfig: {apiKey, authDomain, projectId, storageBucket, messagingSenderId, appId}, vapidKey }`(window와 서비스 워커가 같은 파일을 읽으려고 `self`). 필수 값은 `apiKey`·`projectId`·`messagingSenderId`·`appId`·`vapidKey`. **상태 계약**: `document.body.dataset.fcmState` = `config-missing`(설정 파일 없음) / `config-invalid`(필수 값이 비어 있음, 빈 필드 이름을 화면에 나열) / `unsupported`(보안 컨텍스트 아님, `serviceWorker`·`Notification`·`PushManager` 없음, `isSupported()` false) / `sdk-load-failed`(SDK import 실패) / `ready`. 앞의 세 단계(설정·지원 확인)를 통과하기 전에는 SDK import도 서비스 워커 등록도 외부 요청도 하지 않고 설정 안내만 보여 준다. SDK는 페이지에서 `import("https://www.gstatic.com/firebasejs/{버전}/firebase-app.js")`·`firebase-messaging.js`(모듈)로, 서비스 워커에서 `importScripts(".../firebase-app-compat.js", ".../firebase-messaging-compat.js")`(공식 문서가 번들러 없는 서비스 워커에 보여 주는 compat 방식)로 불러온다. 서비스 워커는 `./firebase-messaging-sw.js`를 명시적으로 등록하고 `getToken(messaging, { vapidKey, serviceWorkerRegistration })`을 쓴다. 인증 헤더는 `access-token`(Bearer 없음), main 주소 기본값은 `http://localhost:8080`(이 PC는 8090). 실행: 레포 루트에서 `python3 -m http.server 5500 -d tools/fcm-test`(Windows는 `python`) → `http://localhost:5500`(127.0.0.1은 다른 origin이라 CORS에 걸림).

**1차 작업 중 검증 (Firebase 호출 없음)**: `FCM_ENABLED=false`로 기동해 발송 단계가 오류 없이 건너뛰어지는지, 발송 로직 단위 테스트(메시지·WebpushConfig 구성, 토큰 조회, 실패 처리)가 통과하는지. 테스트 페이지(R-80)는 설정 파일이 없을 때 안내만 표시하는지까지.

**키 세팅 후 확인 (R-91, 사용자)**: `tools/fcm-test`(R-80)에서 토큰 발급 → `POST /notification/new` → `POST /alarm/send-fcm/{memberId}`(local 프로필) → 브라우저 알림 수신.

> ⚠️ **HTTPS 미적용(P6)과의 관계**: 브라우저의 Service Worker·Push API는 보안 컨텍스트에서만 동작합니다. `localhost`는 예외라 **로컬에서는 문제없지만, HTTP 도메인으로 배포하면 웹푸시가 동작하지 않습니다.** 배포 환경에서 웹푸시가 필요해지면 HTTPS 적용 여부를 다시 결정해야 합니다 (O-7).

## 3. 공공데이터포털 Lost112 API

**현재 코드 (R-32에서 재작성)**: `police/client/Lost112Client`(서비스별 경로, 키 한 번 인코딩) → `Lost112XmlParser`(XXE 방지, resultCode·게이트웨이 오류 판정) → `police/service/PoliceDataNormalizer` → `PoliceAcquiredDataIndexer`(bulk upsert, 문서 ID = `atcId`)를 `Lost112CollectService`가 **페이지마다** 반복한다 (경찰청 → 포털기관, 최근 `LOST112_COLLECT_DAYS`일, `LOST112_PAGE_SIZE`건/페이지, 안전 상한 1,000페이지). 인덱스를 지우지 않고, 한 서비스가 실패해도 다른 서비스는 계속한다. 실행은 `POST /search/save`(200 요약 / 키 없음 503 / 전부 실패 502)와 `policeJob`의 수집 스텝(R-34). 팀 코드(전체 삭제 후 모든 페이지를 줄 단위 문자열로 모아 파싱, `numOfRows=30000`, 시작일 `20240101` 고정)는 제거.

| 서비스 | 경로 | 오퍼레이션 | 내용 |
|---|---|---|---|
| 경찰청 습득물 정보 조회 | `http(s)://apis.data.go.kr/1320000/LosfundInfoInqireService` | `getLosfundInfoAccToClAreaPd` | 경찰관서 보관 습득물 (분류·지역·기간별) |
| 경찰청 포털기관 습득물 정보 조회 | `http(s)://apis.data.go.kr/1320000/LosPtfundInfoInqireService` | `getPtLosfundInfoAccToClAreaPd` | 포털기관(지하철·공항 등) 보관 습득물 |

- 요청 파라미터: `serviceKey`, `pageNo`, `numOfRows`(팀은 30000), `PRDT_CL_CD_01`, `PRDT_CL_CD_02`, `CLR_CD`(포털기관, 경찰청은 `FD_COL_CD` — 2026-10-01 명세 확인, §8), `START_YMD`, `END_YMD`, `N_FD_LCT_CD`
- 사용 응답 필드: `atcId`, `depPlace`, `fdFilePathImg`, `fdPrdtNm`, `fdSbjt`, `fdYmd`, `prdtClNm` (팀 코드는 색상 `clrNm`을 `fdSbjt`를 '색' 기준으로 잘라 추출 — 명세상 응답에 `clrNm`이 있음, §8)

**발급 (U-05)** — 사용자가 1차 작업 완료 후 진행 (D-37, D-38).
1. data.go.kr 로그인 → 위 두 서비스를 각각 **활용신청** (포털에서 서비스 경로로 검색해 정확한 이름 확인). 승인에 시간이 걸릴 수 있음.
2. 마이페이지에서 일반 인증키 확인. `.env`의 `LOST112_SERVICE_KEY`에는 **Decoding 키**를 넣습니다. batch가 한 번 URL 인코딩합니다(`+`→`%2B`, `/`→`%2F`, `=`→`%3D`). Encoding 키(`%XX`가 든 값)를 넣어도 한 번 풀어서 같은 요청을 만듭니다 (R-32).
3. 개발계정 **일일 트래픽 한도**를 확인해 수집 주기·기간·페이지 크기를 정함 (O-4).

**명세 확인 (R-32, Claude)**: 공공데이터포털의 서비스 소개·활용가이드(명세 문서)를 열람해 2024년 이후 요청 파라미터·응답 필드·오류 코드 변경 여부를 확인하고 §8에 기록합니다. 로그인이 필요해 열람할 수 없으면 팀 코드의 파싱 필드를 기준으로 하고, 불확실한 점을 §8에 적어 R-91에서 확인합니다.

**1차 작업 중 검증 (API 호출 없음)**: 명세의 응답 예시로 만든 XML 픽스처로 파싱·bulk 인덱싱 테스트, mock 서버로 페이지 순회·오류 응답(키 오류, 트래픽 초과) 처리 테스트, 샘플 문서 적재 후 main 목록 조회.

**키 세팅 후 확인 (R-91, 사용자)**: `LOST112_COLLECT_ENABLED=true` → batch 수집 실행 후 `GET {batch}/search/total` > 0, main `GET /acquisitions/lost112` 조회.

## 4. Naver 로그인

> **1차 범위에서 제외, 추후 진행 (D-50, 2026-09-30)**: 공식 명세를 열람할 수 없어 사용자가 보류를 결정. 아래는 원래 계획. 현재 코드의 문제 목록은 [08 R-25](08-work-plan.md#phase-2--main-복구).

**현재 코드**: main `MemberCommandController` — `GET /members/login`(Naver가 code를 넘겨주는 콜백, code를 JSON으로 반환), `GET /members/after-login?code=`(토큰 교환 → 회원 조회/가입 → JWT 발급). `NaverOAuthProvider`가 `nid.naver.com/oauth2.0/token`, `openapi.naver.com/v1/nid/me` 호출. 회원 정보로 **휴대전화번호, 연령대, 성별**을 사용. `state`가 상수 `"test"`로 고정(개선 권장).

**발급 (U-06)**
1. Naver Developers → 애플리케이션 등록 → 사용 API "네이버 로그인" → 제공 정보: 휴대전화번호, 연령대, 성별.
2. 서비스 환경 PC 웹: 서비스 URL `http://localhost:8080`, Callback URL `http://localhost:8080/members/login`.
3. Client ID/Secret을 `.env`에. **`Chore/10-reset_env`에 커밋된 옛 Secret은 재발급(U-01).**
4. 검수 전("개발 중")에는 멤버관리에 등록한 네이버 ID만 로그인 가능.

**1차 작업 중 검증 (Naver 호출 없음, R-25)**: 공식 문서(네이버 로그인 API 명세)의 토큰 발급·프로필 조회 응답 예시와 오류 응답을 mock 서버로 재현해, 토큰 교환 → 프로필 조회 → 회원 조회/가입 → JWT 발급까지 계약 테스트. 키 미설정 시 "설정 필요" 오류 응답.

**키 세팅 후 확인 (R-91, 사용자)**: 브라우저에서 `https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id={ID}&redirect_uri={URI}&state=test` → 콜백에서 code 확인 → `GET /members/after-login?code=…`로 JWT 발급.

## 5. VWorld

**현재 코드**: main `LocationController` — `GET /location/search`(장소 검색 `api.vworld.kr/req/search`), `GET /location/address`(주소→좌표 `api.vworld.kr/req/address`). 키 하드코딩(K-04).

**발급 (U-07)**: vworld.kr 회원가입 → 오픈API 인증키 발급 (검색 API 2.0, 주소→좌표 변환 API 2.0), 서비스 URL `http://localhost` 등록. 키 유효기간 확인.

**1차 작업 중 검증 (VWorld 호출 없음, R-26)**: 공식 문서(검색 API 2.0, 주소→좌표 변환 API 2.0)의 요청 파라미터·응답 예시·오류 응답을 mock 서버로 재현해 `/location/search`, `/location/address` 계약 테스트. 키 미설정 시 "설정 필요" 오류 응답.

**구현 (R-26, 2026-09-30)**: `LocationController`가 기존 요청 파라미터 그대로 `RestTemplate`(이 용도 전용, `RestTemplateBuilder`로 연결 3s·읽기 5s) + `UriComponentsBuilder`로 호출하고 **VWorld 응답 JSON을 그대로** 돌려준다(`response.status=ERROR`도 본문 그대로 200 — 프론트가 원본 구조를 씀, 서버 로그엔 오류 코드만). `query`/`address` 필수(없으면 400), `size` 1~1000(기본 10)·`page` ≥1(기본 1). 키가 비면 VWorld를 부르지 않고 **503**(D-49, `ExternalServiceNotConfiguredException`), 연결 실패·타임아웃·VWorld HTTP 4xx/5xx는 **502**(`ExternalServiceUnavailableException`) — 둘 다 `common/exception/ExternalServiceExceptionAdvice`(최우선 순위)가 공통 실패 형식으로. 키가 든 URL은 로그·응답에 남기지 않는다. 계약 테스트는 `mockwebserver3`, e2e는 레포 밖 임시 compose 파일 + WireMock + `VWORLD_BASEURL`(relaxed binding)로 확인.

**키 세팅 후 확인 (R-91, 사용자)**: `GET /location/search?query=서울역&page=1&size=10`, `GET /location/address?…`.

## 6. S3 호환 스토리지

- 로컬: SeaweedFS(D-12). 무료, 설정은 [04](04-target-architecture.md), [06](06-db-and-config.md#4-스토리지-s3-호환).
- 배포: AWS S3 — **유료이므로 사용자 판단 (U-08)**. 연동에 필요한 정보·스크립트는 `infra/aws/`에 모아둠 (R-64, 명세는 [09](09-deploy-and-aws.md#4-aws-s3-연동-키트-r-64)). AWS 공식 문서 기준으로 작성하고 AWS는 호출하지 않음 (D-38). 같은 버킷·CORS 명령을 로컬 SeaweedFS(`storage-init`)에서 실행해 명령 형식을 확인. AWS 연결이 필요한 검증은 생략 (D-41).

## 7. 프론트 복구 때 필요한 연동 (1차 범위 외, 기록용)

- Kakao Maps JS SDK(+services: Places, Geocoder), Daum 우편번호 — Kakao Developers 앱의 JavaScript 키, 플랫폼(Web) 도메인 등록 필요. 옛 키는 `front/index.html`에 하드코딩돼 있음(교체 대상).
- Firebase 웹 설정·VAPID 키 — 위 2번에서 발급한 값 재사용.
- Naver 로그인 — authorize URL 생성 시 **client_secret을 넣지 않음** (팀 시절 버그).

## 8. 공식 문서 확인 기록 (D-38)

구현한 R-xx에서 채웁니다. 다음 세션과 사용자가 "어느 명세 기준으로 만들었는지" 알 수 있게, 문서 URL·확인일·반영한 내용과 **불확실해서 R-91에서 확인할 점**을 적습니다.

| 연동 | 공식 문서 | 확인일 | 반영 내용 / 불확실한 점 | R-xx |
|---|---|---|---|---|
| Firebase Admin SDK (Java), FCM HTTP v1 웹푸시 | https://firebase.google.com/docs/admin/setup · https://firebase.google.com/docs/cloud-messaging/send-message · https://firebase.google.com/docs/cloud-messaging/manage-tokens · https://firebase.google.com/docs/cloud-messaging/error-codes (firebase-admin 9.11.0) | 2026-09-30 | 초기화는 파일 경로의 `GoogleCredentials.fromStream` + `FirebaseOptions.builder()`(문서가 허용하는 명시적 경로 방식), 발송은 동기 `send(Message)`. 토큰 삭제는 `UNREGISTERED`(404)만 — 문서는 `INVALID_ARGUMENT`(400)도 무효 토큰 신호로 들지만 "페이로드가 완전히 유효할 때만"이라는 단서가 있어 경고 로그만. 문서가 등록 토큰 대상(`setToken`)을 deprecated로 두고 FID(`setFid`)를 권장하지만, 웹 SDK가 발급해 저장하는 값이 등록 토큰이라 `setToken` 유지. **R-91에서 확인할 점**: 실제 웹 SDK 토큰으로 발송 성공 여부. 웹 SDK도 등록 토큰(`getToken`)을 deprecated로 두고 FID를 권장한다(R-80 행) — 1차는 등록 토큰 유지, FID 전환은 페이지와 함께 1차 이후(D-61) | R-23 |
| Firebase JS SDK 웹 메시징 (`getToken`, 서비스 워커, 수신) | https://firebase.google.com/docs/cloud-messaging/js/client · https://firebase.google.com/docs/cloud-messaging/js/receive · https://firebase.google.com/docs/web/alt-setup · https://firebase.google.com/support/release-notes/js (모두 리다이렉트 없이 열림, 열람만 — gstatic의 SDK 파일은 받아 보지 않음) | 2026-10-01 | SDK 12.19.0 (릴리스 노트의 최신 2026-09-09, alt-setup 예시도 12.19.0). 페이지는 alt-setup의 CDN 모듈 URL 형식 `https://www.gstatic.com/firebasejs/{버전}/firebase-{서비스}.js`를 `import()`로 불러 `initializeApp` → `isSupported()` → `getMessaging`, 토큰은 client 문서의 `getToken(messaging, { vapidKey })`에 `serviceWorkerRegistration`(명시 등록한 `./firebase-messaging-sw.js`, API 레퍼런스 `GetTokenOptions`의 옵션)을 더해 발급(문서는 서비스 워커 파일을 도메인 루트에 둘 것, HTTPS(localhost 제외) 필수라고 함). **client 문서는 등록 토큰 절("Access the registration token")을 deprecated로 표시**한다 — "This feature is deprecated. Use Firebase Installation IDs, as this method will be removed in a future release."(제거 시점·버전은 없음), 권장은 FID 절의 `register(messaging, { vapidKey })` + `onRegistered()`이고 "Don't use both … at the same time". 1차에서는 `getToken`을 유지한다(D-61): 서버(R-23)가 등록 토큰(`setToken`)으로 보내고, SDK 버전이 고정(JS 12.19.0, Admin Java 9.11.0)돼 있으며, FID로 바꾸려면 페이지·서버·`tbl_notification`을 함께 바꿔야 한다. 포그라운드는 receive 문서의 `onMessage(messaging, cb)`. 서비스 워커는 receive 문서가 번들러 없는 방식으로 보여 주는 `importScripts(".../firebase-app-compat.js")`·`firebase-messaging-compat.js`(문서 예시는 10.13.2이고 "최신 버전으로 바꾸라"고 함) + `firebase.initializeApp(config)` + `firebase.messaging()`. receive 문서는 "All messages received while the app is in the background trigger a display notification in the browser"라고 한다(중복을 직접 경고하는 문장은 없음) — 이 문장과 main이 항상 notification 페이로드를 보내는 점에서, `onBackgroundMessage`에서 `showNotification`을 또 부르면 알림이 두 번 뜬다고 판단해 notification이 없는 data 전용 메시지만 직접 표시하게 했다(팀 시절 코드는 무조건 `showNotification`). `notificationclick`은 문서 지침대로 FCM 라이브러리 import보다 먼저 등록(열린 페이지 포커스, 없으면 열기). **R-91에서 확인할 점**: (1) 실제 `getToken` 발급과 `POST /notification/new` 저장 → `/alarm/send-fcm` 후 백그라운드 알림 수신, (2) 알림이 한 번만 뜨는지(자동 표시와 중복 없음)와 포그라운드에서 `onMessage` 로그가 찍히는지, (3) 12.19.0에서 `firebase-app-compat.js`·`firebase-messaging-compat.js` CDN 파일이 실제로 존재하는지(문서 예시는 10.13.2뿐이라 확인 못 함 — 없으면 서비스 워커를 낮은 버전 compat이나 모듈 방식으로 전환), (4) 서버의 `setToken`(등록 토큰) 발송이 웹 SDK 토큰으로 성공하는지. FID 전환(페이지 `register`/`onRegistered` + 서버 `setFid` + 저장 값)은 섞어 쓰지 말라는 문서 지침대로 양쪽을 함께 바꾸는 1차 이후 작업(D-61, 08 "1차 목표 이후"), (5) 서비스 워커가 `firebase-config.js`를 `importScripts`로 읽는 방식이 Chrome·Firefox에서 동작하는지 | R-80 |
| 공공데이터포털 Lost112 API 2종 (서비스 상세 페이지의 요청·응답 표) | https://www.data.go.kr/data/15058696/openapi.do (경찰청_습득물정보 조회 서비스, 참고문서 `NIA-IFT-OpenAPI활용가이드-02.경찰청-습득물정보조회서비스_v4.0.hwp`) · https://www.data.go.kr/data/15057670/openapi.do (경찰청_포털기관 습득물정보 조회 서비스, 참고문서 `…포털기관습득물정보조회서비스_v2.0.hwp`), 두 페이지 수정일 2025-05-12 | 2026-10-01 (메인 세션, 열람만) | 오퍼레이션 `getLosfundInfoAccToClAreaPd` / `getPtLosfundInfoAccToClAreaPd`, XML만. 요청(모두 옵션): `PRDT_CL_CD_01`·`PRDT_CL_CD_02`·`START_YMD`·`END_YMD`(yyyyMMdd)·`N_FD_LCT_CD`·`pageNo`·`numOfRows`, 색상은 경찰청 **`FD_COL_CD`** / 포털기관 `CLR_CD`(팀 코드는 둘 다 `CLR_CD`). 응답: `resultCode`(00)·`resultMsg`, `atcId`, `fdSn`, `prdtClNm`(경찰청 `지갑 > 남성용 지갑`, 포털기관 `지갑>여성지갑`), `clrNm`(`블랙(검정)`, 옵션 — 팀 코드는 `fdSbjt`에서 색을 잘라냄), `fdPrdtNm`, `fdSbjt`, `fdFilePathImg`, `depPlace`, `fdYmd`(경찰청 `2018-06-01`, 포털기관 `20110223`), 포털기관은 `numOfRows`·`pageNo`·`totalCount`도 표에 있음. 개발계정 일일 트래픽: 경찰청 100,000 / **포털기관 10,000**. 에러코드 01·04·05·10·12·20·22(일일 초과)·23(초당 초과)·29·30(미등록 키)·31(기한 만료). **반영 (R-32)**: 보내는 파라미터는 `serviceKey`·`pageNo`·`numOfRows`·`START_YMD`·`END_YMD`뿐(색상·분류·지역은 쓰지 않아 `FD_COL_CD`/`CLR_CD` 차이는 영향 없음). `resultCode` `00` 정상·`03`(표준 NODATA) 빈 페이지·그 밖 실패, `OpenAPI_ServiceResponse`는 `returnReasonCode`·`returnAuthMsg`로 실패, HTTP 비 2xx는 본문을 읽지 않고 `HTTP nnn`. `fdYmd` 두 형식 → `yyyy-MM-dd`, `prdtClNm`은 `>`로 나눠 대·소분류, `clrNm`은 끝이 `(…)`면 괄호 안 값(`블랙(검정)`→`검정`), 없으면 팀 규칙으로 `fdSbjt`에서 추출. 종료: item 0개 또는 `pageNo×numOfRows ≥ totalCount`(totalCount가 없으면 빈 페이지까지). 픽스처는 아래 가정한 표준 구조. **R-91에서 확인할 점**: 페이지에 요청·응답 XML 예시가 없어 표준 구조(`response/header`, `body/items/item`, `totalCount`)와 게이트웨이 오류 형식(`OpenAPI_ServiceResponse/cmmMsgHeader/returnReasonCode`)·그때의 HTTP 상태를 가정, 경찰청 응답에 `totalCount`가 오는지, `numOfRows` 최댓값(서버가 요청보다 적게 주면 종료 판정 때문에 중간에 멈춤 → `LOST112_PAGE_SIZE`를 낮춤), `atcId` 단독 유일성(`fdSn`·두 서비스 사이 중복 — 같은 atcId는 덮어씀), `clrNm`이 `X(Y)` 외 형태, `resultCode` 03 가정, 30일×1,000건/페이지가 포털기관 일일 10,000건 한도 안인지(O-4), 활용가이드(hwp)는 열어 보지 못함 | R-32 |
| 네이버 로그인 API 명세 (토큰 발급, 회원 프로필 조회) | (R-25에서 기록) | | | R-25 |
| VWorld 검색 API 2.0, 주소→좌표 변환 API 2.0 | https://www.vworld.kr/dev/v4dv_search2_s001.do · https://www.vworld.kr/dev/v4dv_geocoderguide2_s001.do | 2026-09-30 | 검색: `/req/search`, 필수 `request=search`·`key`·`query`·`type`, `size` 1~1000(기본 10)·`page`(기본 1)·`crs`(기본 EPSG:4326)·`format`/`errorFormat`, 응답 `response.status`(OK/NOT_FOUND/ERROR)·`record`·`page`·`result.items[]`, 오류 `error.{level,code,text}`(PARAM_REQUIRED, INVALID_TYPE, INVALID_RANGE, INVALID_KEY, INCORRECT_KEY, UNAVAILABLE_KEY, OVER_REQUEST_LIMIT, SYSTEM_ERROR, UNKNOWN_ERROR). 주소→좌표: `/req/address`, 필수 `request=GetCoord`·`key`·`type`(PARCEL/ROAD)·`address`, `refine`·`simple`, 응답 `refined`·`result.point.{x,y}`, 일일 40,000건. 기존 코드의 파라미터 값은 문서와 어긋난 것이 없어 유지. **R-91에서 확인할 점**: 파라미터 이름·값 대소문자(표는 `errorFormat`·`GetCoord`·`ROAD`, 문서 예시와 코드는 소문자), 서버 호출에 도메인 제한이 걸리는지(문서에 `domain` 파라미터는 없고 발급 도메인이 다르면 오류라는 문구만 있음), 문서에 완전한 JSON 응답 예시가 없어 mock 응답은 필드 설명으로 구성, `type=road` 고정이라 지번 주소는 NOT_FOUND일 수 있음(기존 동작) | R-26 |
| AWS SDK for Java 2.x — S3 presigned URL, 엔드포인트 설정, 자격증명 체인 | https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3-presign.html · https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/configure-service-endpoint.html · https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html (SDK 2.55.8) | 2026-09-30 | `S3Presigner.presignPutObject` + `PutObjectPresignRequest.signatureDuration`, 서명된 헤더(`PresignedPutObjectRequest.signedHeaders()`)는 클라이언트가 같은 값으로 보내야 해서 presign 응답 `headers`로 줌. `endpointOverride` + path-style(`S3Client`는 `forcePathStyle`, Presigner는 `S3Configuration.pathStyleAccessEnabled`). 기본 자격증명 체인(환경변수 → … → EC2 인스턴스 프로파일). AWS 설정의 presigned URL 호스트(`{bucket}.s3.ap-northeast-2.amazonaws.com`)는 가짜 자격증명으로 오프라인 단위 테스트, 실제 업로드는 로컬 SeaweedFS로 확인. **배포 시 확인할 점**: IAM Role로 presign한 URL로 브라우저 PUT, `HeadObject` 권한(06 §4) | R-24 |
| AWS CLI `s3api`, S3 Block Public Access·Object Ownership·HeadObject 권한, IAM 정책·EC2 Role | https://docs.aws.amazon.com/AmazonS3/latest/API/API_HeadObject.html · https://docs.aws.amazon.com/cli/latest/reference/s3api/create-bucket.html · .../s3api/head-bucket.html · .../s3api/put-public-access-block.html · .../s3api/put-bucket-cors.html · .../s3api/put-bucket-policy.html · https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-control-block-public-access.html · https://docs.aws.amazon.com/AmazonS3/latest/userguide/about-object-ownership.html · https://docs.aws.amazon.com/cli/latest/reference/iam/create-role.html · .../iam/put-role-policy.html · .../iam/create-instance-profile.html · .../iam/add-role-to-instance-profile.html · .../ec2/associate-iam-instance-profile.html · .../sts/get-caller-identity.html · https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_use_switch-role-ec2.html (CLI 레퍼런스 2.37.7) | 2026-10-01 | 새 버킷은 Block Public Access 4개가 모두 켜져 있고 Object Ownership 기본값은 BucketOwnerEnforced(ACL 비활성). 공개 읽기 **버킷 정책**을 넣으려면 `BlockPublicPolicy`·`RestrictPublicBuckets`를 꺼야 하고 `BlockPublicAcls`·`IgnorePublicAcls`는 켜 둠(`put-public-access-block`, `s3:PutBucketPublicAccessBlock`). 리전이 us-east-1이 아니면 `create-bucket`에 `LocationConstraint` 필요. HeadObject는 `s3:GetObject` 필요, 없는 객체는 `s3:ListBucket`이 있으면 404, 없으면 403(`s3:prefix` 조건과의 관계는 문서에 없음 → 조건을 붙이지 않음). `put-role-policy`는 같은 이름이면 덮어씀(재실행 안전). Instance Profile당 Role 1개. CLI로 만들면 Role과 Instance Profile을 따로 만들고 연결해야 함(콘솔은 자동). 신뢰 정책 `ec2.amazonaws.com`의 `sts:AssumeRole`. EC2 연결은 `ec2 associate-iam-instance-profile`(`iam:PassRole` 필요). **배포 시 확인할 점**: EC2 IMDS PUT 응답 hop limit이 2인지(컨테이너가 Role 자격증명을 받으려면 필요 — https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/instancedata-data-retrieval.html "Instance metadata access considerations", README ③), `put-bucket-policy`가 계정·조직 수준 Block Public Access에 막히지 않는지, Role로 서명한 presigned PUT이 브라우저에서 되는지, 없는 key의 `HeadObject`가 404인지(`infra/aws/README.md` ⑤) | R-64 |
| Spring Boot 3.5 HTTP 클라이언트 속성, Micrometer `MeterFilter`, Spring Batch 지표 중복 이슈 (외부 연동 아님 — 설정 근거 기록) | https://docs.spring.io/spring-boot/3.5/reference/io/rest-client.html · https://docs.spring.io/spring-boot/3.5/reference/actuator/metrics.html#actuator.metrics.customizing · https://github.com/spring-projects/spring-batch/issues/4753 | 2026-10-01 | `spring.http.client.{connect,read}-timeout`, `spring.http.reactiveclient.connect-timeout`(`spring-boot-autoconfigure-3.5.16` metadata로도 확인), `MeterFilter` 빈은 Boot가 레지스트리에 자동 적용, #4753은 6.0.0-M4에서 해결(5.2.x 백포트 확인 못 함) | R-50 |
| AWS CLI `s3 presign` | https://docs.aws.amazon.com/cli/latest/reference/s3/presign.html (AWS CLI 2.37.6) | 2026-09-30 | GET용 presigned URL만 생성함 ("retrieve the S3 object with an HTTP GET request", 옵션은 `--expires-in`뿐, 메서드 지정 없음) → R-13은 presigned GET까지 확인하고 presigned PUT은 R-24(AWS SDK v2 `S3Presigner`)에서 확인 (D-42) | R-13 |

## 9. 키 세팅 체크리스트 (R-81에서 완성, R-91에서 사용자가 사용)

1차 작업이 끝난 뒤 사용자가 이 표만 보고 키를 채우면 되도록 R-81에서 최종 정리합니다. 채운 뒤 `docker compose up -d`(키 변경 시 해당 앱 재시작) → `tools/verify-external/verify.sh`로 확인합니다.

| 연동 | 발급 (U-xx) | 채울 곳 | 켜는 스위치 | 콘솔에 등록할 값 | 확인 (R-91) |
|---|---|---|---|---|---|
| Naver 로그인 (**추후**, D-50) | U-06 (+ U-01 재발급) | `.env`: `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`, `NAVER_REDIRECT_URI` | – | Callback URL `http://localhost:8080/members/login` | §4 |
| VWorld | U-07 | `.env`: `VWORLD_API_KEY` | – | 서비스 URL `http://localhost` | §5 |
| Lost112 | U-05 | `.env`: `LOST112_SERVICE_KEY`(Decoding 키) | `LOST112_COLLECT_ENABLED=true` | – | §3 |
| FCM (서버) | U-04 | `secrets/firebase-adminsdk.json` | `FCM_ENABLED=true` | – | §2 |
| FCM (테스트 페이지) | U-04 | `tools/fcm-test/firebase-config.js`(웹앱 설정 + VAPID 공개키) | – | – | §2 |
| AWS S3 (배포 시) | U-08 | `.env`: `STORAGE_*` 값 변경, EC2 IAM Role | – | [09 §4](09-deploy-and-aws.md#4-aws-s3-연동-키트-r-64) | `infra/aws/README.md` |
