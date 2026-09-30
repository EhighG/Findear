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

**1차 작업 중 검증 (Firebase 호출 없음)**: `FCM_ENABLED=false`로 기동해 발송 단계가 오류 없이 건너뛰어지는지, 발송 로직 단위 테스트(메시지·WebpushConfig 구성, 토큰 조회, 실패 처리)가 통과하는지. 테스트 페이지(R-80)는 설정 파일이 없을 때 안내만 표시하는지까지.

**키 세팅 후 확인 (R-91, 사용자)**: `tools/fcm-test`(R-80)에서 토큰 발급 → `POST /notification/new` → `POST /alarm/send-fcm/{memberId}`(local 프로필) → 브라우저 알림 수신.

> ⚠️ **HTTPS 미적용(P6)과의 관계**: 브라우저의 Service Worker·Push API는 보안 컨텍스트에서만 동작합니다. `localhost`는 예외라 **로컬에서는 문제없지만, HTTP 도메인으로 배포하면 웹푸시가 동작하지 않습니다.** 배포 환경에서 웹푸시가 필요해지면 HTTPS 적용 여부를 다시 결정해야 합니다 (O-7).

## 3. 공공데이터포털 Lost112 API

**현재 코드(팀 batch)**: `police/job/tasklet/PoliceDataSaveTasklet`, `police/service/PoliceAcquiredDataService`에서 두 API를 호출해 XML을 파싱 → ES `police_acquired_data`에 저장.

| 서비스 | 경로 | 오퍼레이션 | 내용 |
|---|---|---|---|
| 경찰청 습득물 정보 조회 | `http(s)://apis.data.go.kr/1320000/LosfundInfoInqireService` | `getLosfundInfoAccToClAreaPd` | 경찰관서 보관 습득물 (분류·지역·기간별) |
| 경찰청 포털기관 습득물 정보 조회 | `http(s)://apis.data.go.kr/1320000/LosPtfundInfoInqireService` | `getPtLosfundInfoAccToClAreaPd` | 포털기관(지하철·공항 등) 보관 습득물 |

- 요청 파라미터: `serviceKey`, `pageNo`, `numOfRows`(팀은 30000), `PRDT_CL_CD_01`, `PRDT_CL_CD_02`, `CLR_CD`, `START_YMD`, `END_YMD`, `N_FD_LCT_CD`
- 사용 응답 필드: `atcId`, `depPlace`, `fdFilePathImg`, `fdPrdtNm`, `fdSbjt`, `fdYmd`, `prdtClNm` (색상 `clrNm`은 `fdSbjt`를 '색' 기준으로 잘라 추출)

**발급 (U-05)** — 사용자가 1차 작업 완료 후 진행 (D-37, D-38).
1. data.go.kr 로그인 → 위 두 서비스를 각각 **활용신청** (포털에서 서비스 경로로 검색해 정확한 이름 확인). 승인에 시간이 걸릴 수 있음.
2. 마이페이지에서 일반 인증키 확인. `.env`의 `LOST112_SERVICE_KEY`에는 **Decoding 키**를 넣고, 코드(R-32)에서 URL 인코딩합니다. (팀 코드는 키를 인코딩 없이 URL에 붙였으므로, 코드 수정 전에는 Encoding 키를 써야 함)
3. 개발계정 **일일 트래픽 한도**를 확인해 수집 주기·기간·페이지 크기를 정함 (O-4).

**명세 확인 (R-32, Claude)**: 공공데이터포털의 서비스 소개·활용가이드(명세 문서)를 열람해 2024년 이후 요청 파라미터·응답 필드·오류 코드 변경 여부를 확인하고 §8에 기록합니다. 로그인이 필요해 열람할 수 없으면 팀 코드의 파싱 필드를 기준으로 하고, 불확실한 점을 §8에 적어 R-91에서 확인합니다.

**1차 작업 중 검증 (API 호출 없음)**: 명세의 응답 예시로 만든 XML 픽스처로 파싱·bulk 인덱싱 테스트, mock 서버로 페이지 순회·오류 응답(키 오류, 트래픽 초과) 처리 테스트, 샘플 문서 적재 후 main 목록 조회.

**키 세팅 후 확인 (R-91, 사용자)**: `LOST112_COLLECT_ENABLED=true` → batch 수집 실행 후 `GET {batch}/search/total` > 0, main `GET /acquisitions/lost112` 조회.

## 4. Naver 로그인

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

**키 세팅 후 확인 (R-91, 사용자)**: `GET /location/search?query=서울역&page=1&size=10`, `GET /location/address?…`.

## 6. S3 호환 스토리지

- 로컬: SeaweedFS(D-12). 무료, 설정은 [04](04-target-architecture.md), [06](06-db-and-config.md#4-스토리지-s3-호환).
- 배포: AWS S3 — **유료이므로 사용자 판단 (U-08)**. 연동에 필요한 정보·스크립트는 `infra/aws/`에 모아둠 (R-64, 명세는 [09](09-deploy-and-aws.md#4-aws-s3-연동-키트)). AWS 공식 문서 기준으로 작성하고 AWS는 호출하지 않음 (D-38). 같은 버킷·CORS 명령을 로컬 SeaweedFS(`storage-init`)에서 실행해 명령 형식을 확인. AWS 연결이 필요한 검증은 생략 (D-41).

## 7. 프론트 복구 때 필요한 연동 (1차 범위 외, 기록용)

- Kakao Maps JS SDK(+services: Places, Geocoder), Daum 우편번호 — Kakao Developers 앱의 JavaScript 키, 플랫폼(Web) 도메인 등록 필요. 옛 키는 `front/index.html`에 하드코딩돼 있음(교체 대상).
- Firebase 웹 설정·VAPID 키 — 위 2번에서 발급한 값 재사용.
- Naver 로그인 — authorize URL 생성 시 **client_secret을 넣지 않음** (팀 시절 버그).

## 8. 공식 문서 확인 기록 (D-38)

구현한 R-xx에서 채웁니다. 다음 세션과 사용자가 "어느 명세 기준으로 만들었는지" 알 수 있게, 문서 URL·확인일·반영한 내용과 **불확실해서 R-91에서 확인할 점**을 적습니다.

| 연동 | 공식 문서 | 확인일 | 반영 내용 / 불확실한 점 | R-xx |
|---|---|---|---|---|
| Firebase Admin SDK (Java), FCM HTTP v1 웹푸시 | https://firebase.google.com/docs/admin/setup · https://firebase.google.com/docs/cloud-messaging/send-message · https://firebase.google.com/docs/cloud-messaging/manage-tokens · https://firebase.google.com/docs/cloud-messaging/error-codes (firebase-admin 9.11.0) | 2026-09-30 | 초기화는 파일 경로의 `GoogleCredentials.fromStream` + `FirebaseOptions.builder()`(문서가 허용하는 명시적 경로 방식), 발송은 동기 `send(Message)`. 토큰 삭제는 `UNREGISTERED`(404)만 — 문서는 `INVALID_ARGUMENT`(400)도 무효 토큰 신호로 들지만 "페이로드가 완전히 유효할 때만"이라는 단서가 있어 경고 로그만. 문서가 등록 토큰 대상(`setToken`)을 deprecated로 두고 FID(`setFid`)를 권장하지만, 웹 SDK가 발급해 저장하는 값이 등록 토큰이라 `setToken` 유지. **R-91에서 확인할 점**: 실제 웹 SDK 토큰으로 발송 성공 여부, R-80 테스트 페이지가 FID를 쓰게 되면 `setFid` 전환 검토 | R-23 |
| Firebase JS SDK 웹 메시징 (`getToken`, 서비스 워커) | (R-80에서 기록) | | | R-80 |
| 공공데이터포털 Lost112 API 2종 활용가이드 | (R-32에서 기록) | | | R-32 |
| 네이버 로그인 API 명세 (토큰 발급, 회원 프로필 조회) | (R-25에서 기록) | | | R-25 |
| VWorld 검색 API 2.0, 주소→좌표 변환 API 2.0 | (R-26에서 기록) | | | R-26 |
| AWS CLI `s3api`, IAM 정책·EC2 Role | (R-64에서 기록) | | | R-64 |
| AWS CLI `s3 presign` | https://docs.aws.amazon.com/cli/latest/reference/s3/presign.html (AWS CLI 2.37.6) | 2026-09-30 | GET용 presigned URL만 생성함 ("retrieve the S3 object with an HTTP GET request", 옵션은 `--expires-in`뿐, 메서드 지정 없음) → R-13은 presigned GET까지 확인하고 presigned PUT은 R-24(AWS SDK v2 `S3Presigner`)에서 확인 (D-42) | R-13 |

## 9. 키 세팅 체크리스트 (R-81에서 완성, R-91에서 사용자가 사용)

1차 작업이 끝난 뒤 사용자가 이 표만 보고 키를 채우면 되도록 R-81에서 최종 정리합니다. 채운 뒤 `docker compose up -d`(키 변경 시 해당 앱 재시작) → `tools/verify-external/verify.sh`로 확인합니다.

| 연동 | 발급 (U-xx) | 채울 곳 | 켜는 스위치 | 콘솔에 등록할 값 | 확인 (R-91) |
|---|---|---|---|---|---|
| Naver 로그인 | U-06 (+ U-01 재발급) | `.env`: `NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`, `NAVER_REDIRECT_URI` | – | Callback URL `http://localhost:8080/members/login` | §4 |
| VWorld | U-07 | `.env`: `VWORLD_API_KEY` | – | 서비스 URL `http://localhost` | §5 |
| Lost112 | U-05 | `.env`: `LOST112_SERVICE_KEY`(Decoding 키) | `LOST112_COLLECT_ENABLED=true` | – | §3 |
| FCM (서버) | U-04 | `secrets/firebase-adminsdk.json` | `FCM_ENABLED=true` | – | §2 |
| FCM (테스트 페이지) | U-04 | `tools/fcm-test/firebase-config.js`(웹앱 설정 + VAPID 공개키) | – | – | §2 |
| AWS S3 (배포 시) | U-08 | `.env`: `STORAGE_*` 값 변경, EC2 IAM Role | – | [09 §4](09-deploy-and-aws.md#4-aws-s3-연동-키트) | `infra/aws/README.md` |
