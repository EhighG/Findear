# 05. 외부 연동

> 원칙: 기능상 필요한 **무료 연동은 전부 복구**, 유료는 사용자 보고 후 판단 (P4).
> 발급받은 값은 `.env` 또는 `secrets/`에만 둡니다. **절대 커밋 금지** (public 레포).
> 환경변수 이름은 [06-db-and-config.md](06-db-and-config.md#6-환경변수-전체-목록)와 일치해야 합니다.

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

**검증**: `tools/fcm-test`(R-80)에서 토큰 발급 → `POST /notification/new` → `POST /alarm/send-fcm/{memberId}`(local 프로필) → 브라우저 알림 수신.

> ⚠️ **HTTPS 미적용(P6)과의 관계**: 브라우저의 Service Worker·Push API는 보안 컨텍스트에서만 동작합니다. `localhost`는 예외라 **로컬에서는 문제없지만, HTTP 도메인으로 배포하면 웹푸시가 동작하지 않습니다.** 배포 환경에서 웹푸시가 필요해지면 HTTPS 적용 여부를 다시 결정해야 합니다 (O-7).

## 3. 공공데이터포털 Lost112 API

**현재 코드(팀 batch)**: `police/job/tasklet/PoliceDataSaveTasklet`, `police/service/PoliceAcquiredDataService`에서 두 API를 호출해 XML을 파싱 → ES `police_acquired_data`에 저장.

| 서비스 | 경로 | 오퍼레이션 | 내용 |
|---|---|---|---|
| 경찰청 습득물 정보 조회 | `http(s)://apis.data.go.kr/1320000/LosfundInfoInqireService` | `getLosfundInfoAccToClAreaPd` | 경찰관서 보관 습득물 (분류·지역·기간별) |
| 경찰청 포털기관 습득물 정보 조회 | `http(s)://apis.data.go.kr/1320000/LosPtfundInfoInqireService` | `getPtLosfundInfoAccToClAreaPd` | 포털기관(지하철·공항 등) 보관 습득물 |

- 요청 파라미터: `serviceKey`, `pageNo`, `numOfRows`(팀은 30000), `PRDT_CL_CD_01`, `PRDT_CL_CD_02`, `CLR_CD`, `START_YMD`, `END_YMD`, `N_FD_LCT_CD`
- 사용 응답 필드: `atcId`, `depPlace`, `fdFilePathImg`, `fdPrdtNm`, `fdSbjt`, `fdYmd`, `prdtClNm` (색상 `clrNm`은 `fdSbjt`를 '색' 기준으로 잘라 추출)

**발급 (U-05)** — 사용자가 1차 작업 완료 후 진행 (D-37). 그 전까지 R-32는 명세서 예시 기반 XML 픽스처와 샘플 문서로 확인합니다.
1. data.go.kr 로그인 → 위 두 서비스를 각각 **활용신청** (포털에서 서비스 경로로 검색해 정확한 이름 확인). 승인에 시간이 걸릴 수 있으니 미리 신청.
2. 마이페이지에서 일반 인증키 확인. `.env`의 `LOST112_SERVICE_KEY`에는 **Decoding 키**를 넣고, 코드(R-32)에서 URL 인코딩합니다. (팀 코드는 키를 인코딩 없이 URL에 붙였으므로, 코드 수정 전에는 Encoding 키를 써야 함)
3. 개발계정 **일일 트래픽 한도**를 확인해 수집 주기·기간·페이지 크기를 정함 (O-4).
4. 명세 문서를 내려받아 2024년 이후 스펙 변경 여부 확인.

**검증**: batch 수집 실행 후 `GET {batch}/search/total` > 0, main `GET /acquisitions/lost112` 조회.

## 4. Naver 로그인

**현재 코드**: main `MemberCommandController` — `GET /members/login`(Naver가 code를 넘겨주는 콜백, code를 JSON으로 반환), `GET /members/after-login?code=`(토큰 교환 → 회원 조회/가입 → JWT 발급). `NaverOAuthProvider`가 `nid.naver.com/oauth2.0/token`, `openapi.naver.com/v1/nid/me` 호출. 회원 정보로 **휴대전화번호, 연령대, 성별**을 사용. `state`가 상수 `"test"`로 고정(개선 권장).

**발급 (U-06)**
1. Naver Developers → 애플리케이션 등록 → 사용 API "네이버 로그인" → 제공 정보: 휴대전화번호, 연령대, 성별.
2. 서비스 환경 PC 웹: 서비스 URL `http://localhost:8080`, Callback URL `http://localhost:8080/members/login`.
3. Client ID/Secret을 `.env`에. **`Chore/10-reset_env`에 커밋된 옛 Secret은 재발급(U-01).**
4. 검수 전("개발 중")에는 멤버관리에 등록한 네이버 ID만 로그인 가능.

**검증**: 브라우저에서 `https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id={ID}&redirect_uri={URI}&state=test` → 콜백에서 code 확인 → `GET /members/after-login?code=…`로 JWT 발급.

## 5. VWorld

**현재 코드**: main `LocationController` — `GET /location/search`(장소 검색 `api.vworld.kr/req/search`), `GET /location/address`(주소→좌표 `api.vworld.kr/req/address`). 키 하드코딩(K-04).

**발급 (U-07)**: vworld.kr 회원가입 → 오픈API 인증키 발급 (검색 API 2.0, 주소→좌표 변환 API 2.0), 서비스 URL `http://localhost` 등록. 키 유효기간 확인.

**검증**: `GET /location/search?query=서울역&page=1&size=10`.

## 6. S3 호환 스토리지

- 로컬: SeaweedFS(D-12). 무료, 설정은 [04](04-target-architecture.md), [06](06-db-and-config.md#4-스토리지-s3-호환).
- 배포: AWS S3 — **유료이므로 사용자 판단 (U-08)**. 연동에 필요한 정보·스크립트는 `infra/aws/`에 모아둠 (R-64, 명세는 [09](09-deploy-and-aws.md#4-aws-s3-연동-키트)).

## 7. 프론트 복구 때 필요한 연동 (1차 범위 외, 기록용)

- Kakao Maps JS SDK(+services: Places, Geocoder), Daum 우편번호 — Kakao Developers 앱의 JavaScript 키, 플랫폼(Web) 도메인 등록 필요. 옛 키는 `front/index.html`에 하드코딩돼 있음(교체 대상).
- Firebase 웹 설정·VAPID 키 — 위 2번에서 발급한 값 재사용.
- Naver 로그인 — authorize URL 생성 시 **client_secret을 넣지 않음** (팀 시절 버그).
