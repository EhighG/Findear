# 07. 서버 간 API 계약

> 공통 응답 형식(main·batch): 성공 `{"status": int, "message": str, "result": any}`, 실패 `{"status": int, "message": str}`.
> main의 실패 응답 (R-27, D-51): 본문 `status` = HTTP 상태. 인증 없음·잘못된 토큰 401, 권한 없음 403(본인·작성자·참여자 검사 포함), 입력·도메인 오류 400, 없는 경로 404, 메서드 405, 외부 연동 키 미설정 503·외부 호출 실패 502(D-49), 그 밖의 오류 500(메시지 고정, 원인은 서버 로그에만). "local" 표시 엔드포인트는 `local` 프로필에서만 존재한다(D-26).
> **JSON 필드명 주의**: Lombok 필드 `xPos`/`yPos`는 getter `getXPos()` 때문에 JSON에서 **`xpos`/`ypos`**가 됩니다. match mock은 둘 다 받도록 구현하세요.

## 1. 호출 관계
```
main ──GET/POST──▶ batch   (Lost112 목록·총개수·스크랩, 분실물 등록 직후 매칭, 매칭 목록)
main ──POST──────▶ match   (/process: 습득물 자동채움)
batch ─POST──────▶ match   (/matching/findear, /matching/lost)
```

## 2. main → batch

| 용도 | 메서드·경로 | 요청 | 응답 `result` |
|---|---|---|---|
| Lost112 습득물 목록 | `GET {batch}/search?page&size&category&startDate&endDate&keyword` | query | `[{id, atcId, depPlace, fdFilePathImg, fdPrdtNm, fdSbjt, clrNm, fdYmd, prdtClNm, mainPrdtClNm, subPrdtClNm}]` |
| Lost112 총개수 | `GET {batch}/search/total` | – | number |
| 스크랩한 Lost112 습득물 | `POST {batch}/police/scrap` | `{"atcIdList": ["…"]}` | 목록과 같은 형태의 배열 |
| 분실물 등록 직후 매칭 | `POST {batch}/findear/matching` | `{lostBoardId, productName, color, categoryName, description, lostAt, xpos, ypos}` | `{findearDatas: [{lostBoardId, acquiredBoardId, similarityRate}], policeDatas: [...]}` — main은 등록 트랜잭션 **커밋 후** 비동기로 요청하고(R-35, D-52와 같은 방식, 대기 상한 `servers.batch-server.matching-timeout` 60s), `findearDatas`·`policeDatas` 중 1건 이상이면 **이벤트의 분실물 id**로 작성자를 조회해 알림 1건(FCM은 R-23 규칙). 실패는 WARN 한 줄, 등록 응답과 무관 |
| 매칭 목록 | `GET {batch}/{findear\|police}/{member\|board}/{id}?page=1&size=6` | – | `{matchingList: [...], totalCount}` → main이 `totalPageNum`으로 변환 |

- findear 매칭 항목: `{findearMatchingLogId, lostBoardId, acquiredBoardId, similarityRate, matchedAt}`
- police 매칭 항목: `{policeMatchingLogId, lostBoardId, similarityRate, matchedAt, acquiredBoardId, atcId, depPlace, fdFilePathImg, fdPrdtNm, fdSbjt, clrNm, fdYmd, mainPrdtClNm}`
- **K-01** (해결, R-22): master main은 Lost112 목록을 `{batch}?page=`, 총개수를 `{batch}/total`로 호출했음 → `/search`, `/search/total`로 수정, 쿼리는 `UriComponentsBuilder`로 한 번만 인코딩. ~~남은 점: `keyword`의 `+`는 인코딩되지 않음~~ → R-35에서 해결: 쿼리 값은 URI 변수로 넣어 `+`·`&`·`=`·`%`·한글을 한 번만 인코딩 (`keyword=a+b` → `a%2Bb` → batch가 `a+b`).
- R-22에서 main의 나머지 호출 경로(`/police/scrap`, `/findear/matching`, 매칭 목록, match `/process`)가 이 표와 같음을 확인. 매칭 요청 DTO의 `xPos`/`yPos`는 JSON에서 `xpos`/`ypos`(숫자)로 나가고 batch는 문자열로 받아 match에 그대로 넘긴다, `lostAt`은 `yyyy-MM-dd` — R-35에서 계약 테스트·e2e로 확인.
- **batch 쪽 (R-30, Boot 3.5 이식 후)**: 응답 모양은 팀 버전 그대로임을 e2e로 확인. 세부:
  - **(R-32)** Lost112 항목(`/search`, `/search/all`, `/police/scrap`, match `/matching/lost` 후보)의 `id`는 **atcId 문자열**(문서 ID = atcId). 없는 값은 `null`(예: `addr`, 사진 없는 `fdFilePathImg`). 목록은 `fdYmd` 내림차순, 같은 날은 `atcId` 내림차순. 카테고리는 `mainPrdtClNm` 정확히 일치. `/police/scrap`은 요청한 atcId 순서대로, 없는 것은 빠짐. match `/matching/lost` 결과의 `acquiredBoardId`도 atcId 문자열(match mock은 `id`가 정수가 아니면 문자열로 돌려줌)
  - Lost112 목록의 `startDate`·`endDate`는 `yyyy-MM-dd`, 양끝 날짜 포함, 둘 다 없으면 오늘까지. 형식이 틀리면 오류
  - 매칭 목록(`/{findear|police}/board/{id}`)의 `totalCount`는 전체 일치 건수이고 페이지는 ES에서 자른다 (팀 코드는 ES 기본 10건 안에서만 잘랐음). `/{…}/member/{id}`는 분실물별 최고 점수 1건씩을 모아 메모리에서 자름(그대로)
  - `/findear/matching` 요청의 `lostAt`은 날짜(`yyyy-MM-dd`)만 받는다 (시각이 붙으면 오류). batch가 match에 보내는 요청은 `lostBoard.xpos`/`ypos` 키, 값은 전부 문자열 — match 계약 픽스처와 키가 같음을 테스트로 확인
  - **(R-33)** 매칭 목록 항목의 `findearMatchingLogId`·`policeMatchingLogId`는 문자열(`"1-3"`, `"1-F2099…"` — `{lostBoardId}-{acquiredBoardId|atcId}`), `matchedAt`은 `yyyy-MM-ddTHH:mm:ss`(KST, 초 단위). 같은 분실물을 다시 매칭하면 그 분실물의 로그가 새 결과로 교체된다(중복 없음, 06 §3 교체 규칙). 로그가 없으면 200 빈 목록·`totalCount` 0. main은 ID를 읽지 않고 `matchedAt`은 문자열 그대로 넘기므로 영향 없음

## 3. batch API 전체 (팀 버전)와 1차 처리

| 메서드·경로 | 용도 | 처리 |
|---|---|---|
| `POST /findear/matching` | 분실물 1건 매칭 (main 호출) | 유지. R-34: Findear·Lost112 매칭을 잡과 같은 서비스로 (후보에서 삭제·반환 완료 제외, `acquiredBoardId` = board_id) |
| `POST /findear/matching/batch` | 전체 분실물 매칭 수동 실행 | 유지 (내부용). R-34: `findearJob` 실행 → 200 `result` = `{jobExecutionId, jobName, status, exitCode, durationMillis, steps: [{stepName, status, exitCode, processed, succeeded, failed, message}]}` (잡이 FAILED여도 200, 상태는 본문), 같은 잡이 실행 중이면 409 |
| `POST /police/matching/batch` | (R-34 신규) `policeJob` 실행 — 수집(`LOST112_COLLECT_ENABLED`일 때) → Lost112 매칭 | 내부용, 응답은 위와 같음 |
| `GET /findear/member/{memberId}`, `GET /findear/board/{lostBoardId}` | Findear 매칭 목록 | 유지 |
| `GET /police/member/{memberId}`, `GET /police/board/{lostBoardId}` | Lost112 매칭 목록 | 유지 |
| `POST /police/scrap` | 스크랩 습득물 조회 | 유지 |
| `GET /search`, `GET /search/total` | Lost112 목록·총개수 | 유지 |
| `POST /search/save` | Lost112 수집 수동 실행 | 유지 (내부용). R-32: 200 `result` = `{startYmd, endYmd, services: [{service, pages, fetched, indexed, skipped, truncated, error}]}`, 키 없음 503, 실패가 있고 한 건도 못 넣으면 502 (`{status, message}`) |
| `GET /findear`, `GET /search/all`, `GET /search/save?page&size`(이름과 달리 조회) | 전체 조회 | local 프로필 한정 |
| `DELETE /findear`, `DELETE /police`, `DELETE /search` | 전체 삭제 | local 프로필 한정 |
| `GET /findear/test-api`, `GET /police/test-api`, `GET /search/test` | 테스트 | 삭제 또는 local 한정 |

batch는 호스트/외부에 공개하지 않습니다(로컬은 127.0.0.1 디버깅용만).

## 4. main 외부 API 목록 (47개, 테스트·프론트 복구 참고)

| base | 엔드포인트 |
|---|---|
| `/members` | `POST /`(가입·local), `PATCH /{id}/role`(본인만), `POST /login`(전화번호 로그인·local), `GET /login`(Naver 콜백), `GET /after-login?code`, `POST /logout`, `PATCH /{id}`(본인만), `PATCH /{id}/delete`(본인만), `POST /token/refresh`, `POST /duplicate`, `GET /{id}`, `GET /token-check`, `GET /?keyword`(회원 검색·local) |
| `/acquisitions` | `POST /`, `PATCH /{boardId}`, `PATCH /{boardId}/delete`, `POST /{boardId}/return`, `PATCH /{boardId}/rollback`, `POST·DELETE /{boardId}/scrap`, `GET /`, `GET /lost112`, `GET /{boardId}`, `GET /lost112/total-page`, `GET /returns/count`, `GET /scraps` |
| `/losts` | `POST /`, `PATCH /{boardId}`, `PATCH /{boardId}/delete`, `GET /`, `GET /{boardId}` |
| `/matchings` | `GET /findear/bests`, `GET /findear/total`, `GET /lost112/bests`, `GET /lost112/total` |
| `/message` | `POST /`, `POST /reply`, `GET /`, `GET /{messageRoomId}` |
| `/alarm` | `GET /subscribe/{memberId}`(SSE), `POST /send-data/{memberId}`(테스트·local), `POST /send-fcm/{memberId}`(테스트·local), `GET /alarm-list`, `GET /{alarmId}` |
| `/notification` | `POST /new` (FCM 토큰 등록) |
| `/location` | `GET /search`, `GET /address` (VWorld 프록시) |
| `/images` (R-24) | `POST /presign` (인증 필요) |

### 이미지 업로드 계약 (R-24, D-13)
1. `POST /images/presign` 요청 `{"contentType": "image/jpeg", "contentLength": 2022}` — 허용 `image/jpeg`·`image/png`·`image/webp`·`image/gif`, 크기 1 ~ 10,485,760. 벗어나면 400, 토큰 없으면 401.
   응답 `result`: `{"key": "images/2026/09/<uuid>.jpg", "uploadUrl": "http://localhost:8333/findear-images/images/…?X-Amz-…", "url": "http://localhost:8333/findear-images/images/….jpg", "expiresAt": "…+09:00", "headers": {"Content-Type": "image/jpeg", "Content-Length": "2022"}}`
2. 클라이언트가 `PUT {uploadUrl}`에 파일 바이트를 보내며 **`headers`를 그대로** 붙인다 (서명에 포함된 값이라 Content-Type·크기·호스트가 다르면 403 `SignatureDoesNotMatch`). 만료 `STORAGE_PRESIGN_EXPIRE_SECONDS`(기본 600초).
3. 게시글 등록·수정 요청에는 URL이 아니라 **`imgKeys`**(위 `key` 목록)를 보낸다 (`POST /acquisitions`, `POST /losts`, `PATCH /acquisitions/{boardId}`, `PATCH /losts/{boardId}`). 각 key는 `images/…` 형식이고 스토리지에 실제로 있어야 하며, 다른 게시글에 붙은 key·중복 key는 400. 습득물은 1개 이상 필수, 분실물은 없어도 됨. 수정에서 `imgKeys`를 주면 이미지가 정확히 그 목록(순서 포함)이 되고, 생략하면 그대로.
4. 조회 응답은 필드 이름이 그대로(`imgUrls`, `thumbnailUrl`)이고 값만 `STORAGE_PUBLIC_BASE_URL/key`로 조립된 공개 URL (매칭·쪽지 응답의 `thumbnailUrl` 포함). DB에는 key만 저장.
5. match `/process`의 `imgUrl`에는 첫 이미지의 공개 URL을 보낸다.

## 5. match mock 동작 명세 (R-40, D-28)

구현: `match/` (Spring Boot 3.5.16, 앱 포트 8084 / 관리 포트 8085). 경로는 팀 시절 그대로이고 접두사가 없다. 설정은 `MATCH_MOCK_SEED`(42)·`MATCH_MOCK_LATENCY_MS`(0)·`MATCH_MOCK_MAX_RESULTS`(100), 범위를 벗어나면(`LATENCY_MS` < 0, `MAX_RESULTS` < 1) 기동 실패.

### 5.0 공통
- 세 POST 엔드포인트 모두 본문 파싱 뒤 `MATCH_MOCK_LATENCY_MS`만큼 기다렸다가 응답한다 (모니터링·지연 확인용, 액추에이터는 제외).
- 요청의 모르는 필드는 무시하고, 문자열 필드에 JSON 숫자가 와도 받는다. 응답은 null 필드도 키를 그대로 둔다.
- **오류 응답은 전부 `{"message": "<한국어 설명>"}`** 한 모양 (팀 시절 400의 `{"error": …}`와 다름): 깨진 JSON·필수값 누락·형식 오류 400, 없는 경로 404, 메서드 405, Accept 불일치 406, Content-Type 415, 그 밖의 예외 500(고정 메시지, 원인은 로그에만). Content-Type은 항상 `application/json`.
- 요청마다 INFO 로그 한 줄(건수 요약), 요청 본문 전체는 남기지 않는다.

### 5.1 `POST /process` (main → match, 습득물 자동채움)
- 요청 `{productName, imgUrl}` — 둘 다 공백이 아닌 문자열 필수, 아니면 400 → 응답 200 `{"message":"success","result":{"category": str,"color": str,"description":[str×5]}}`
- 카테고리 후보(순서 고정): 카드, 지갑, 현금, 의류, 전자기기, 가방, 휴대폰, 증명서, 쇼핑백, 귀금속, 유가증권, 자동차, 서류, 도서용품, 스포츠용품, 컴퓨터, 산업용품, 악기, 기타
- 색상 후보(순서 고정): 검정, 흰, 빨강, 오렌지, 노랑, 초록, 파랑, 갈, 보라, 회, 기타
- 결정적 선택: `h = SHA-256(UTF-8("<productName>|<imgUrl>|<seed>"))`, category = 카테고리[`h[0..3]`(big-endian 부호 없는 정수) mod 19], color = 색상[`h[4..7]` mod 11].
- `description`은 **항상 5개, 서로 다르고 공백 없음** (main이 공백으로 이어 붙여 `ai_description`에 저장하므로): productName을 공백으로 나눈 토큰(중복 제거·순서 유지) 앞에서 최대 5개 → 모자라면 고정 풀(소형, 대형, 가죽, 플라스틱, 금속, 천소재, 무지, 줄무늬, 로고, 낡음, 새것, 사각형, 원형, 지퍼, 끈)에서 `(h[8+i] & 0xFF) mod 15`부터 이미 쓴 값은 건너뛰며(순환) 채운다.
- 팀 계약의 실패 응답 404 `{"message":"GPT api failed"}` 흉내는 만들지 않았다 (필요해지면 추가).
- **main 쪽 동작 (R-41, D-52)**: 습득물 등록 트랜잭션이 **커밋된 뒤** 비동기로 호출한다(`AutoFillRequestListener` → `MatchAutoFillClient`, 롤백이면 호출 없음, 등록 응답은 기다리지 않음). 응답을 `servers.match-server.autofill-timeout`(30s) 안에 받지 못하거나 4xx·5xx·본문 오류·`result: null`이면 WARN 한 줄로 끝낸다. 받은 값은 새 트랜잭션에서 게시글을 다시 읽어 **비어 있는 컬럼(`category_name`·`color`·`ai_description`)만** 채운다 — 그 사이 관리자가 넣은 값은 유지, 삭제된 게시글은 건너뜀, `description`이 비거나 공백뿐이면 `ai_description`은 그대로.

### 5.2 `POST /matching/findear` (batch → match)
- 요청 `{"lostBoard": {lostBoardId, productName, color, categoryName, description, lostAt, xpos, ypos}, "acquiredBoardList": [{acquiredBoardId, productName, color, categoryName, description, xpos, ypos, registeredAt}]}` — **값이 전부 문자열**로 옴(batch DTO가 String). `xPos`/`yPos`(camelCase)도 받는다.
- 400: `lostBoard` 없음, `lostBoardId`가 없거나 정수가 아님, 목록 원소 null, `acquiredBoardId`가 없거나 정수가 아님. `lostBoard` 검사가 먼저라 후보 목록이 비어 있어도 `lostBoard`가 없으면 400.
- `acquiredBoardList`가 없거나(null) 비면 200 `{"message": "해당 분실물과 매칭 가능한 findear 데이터가 없습니다.", "result": null}` (`result` 키가 null로 존재 — batch는 이때 빈 목록으로 처리).
- 정상 200 `{"message": "해당 분실물과 findear 데이터와의 매칭이 완료되었습니다", "result": [{"lostBoardId": int, "acquiredBoardId": int, "similarityRate": float}]}` — 점수 [0,1]로 자르기 → 소수 5자리 HALF_UP → 내림차순(같은 점수는 입력 순서 유지) → 최대 `MATCH_MOCK_MAX_RESULTS`(100)개.

### 5.3 `POST /matching/lost` (batch → match, Lost112 매칭)
- 요청 `{"lostBoard": {...위와 동일}, "acquiredBoardList": [{id, atcId, depPlace, fdFilePathImg, fdPrdtNm, fdSbjt, clrNm, fdYmd, mainPrdtClNm}]}`. 400 조건은 5.2의 `lostBoard`·원소 null과 같고 `id`는 검사하지 않는다.
- 응답 `result`: `[{lostBoardId, acquiredBoardId, similarityRate, atcId, depPlace, fdFilePathImg, fdPrdtNm, fdSbjt, clrNm, fdYmd, mainPrdtClNm}]` — `acquiredBoardId`는 `id`가 정수면 숫자, 아니면 원래 문자열, 없으면 null. 나머지는 입력값을 그대로 되돌려준다(null은 null). 메시지는 "해당 분실물과 lost112 데이터와의 매칭이 완료되었습니다" / 빈 목록 "해당 분실물과 매칭 가능한 lost112 데이터가 없습니다.", 정렬·상한·반올림은 5.2와 동일.
- 참고 (R-35): 팀 batch 코드는 결과의 `fdFilePathImg`·`atcId` 등을 null 검사 없이 `toString()`하므로, 입력에 null이 있으면 batch에서 NPE가 날 수 있다.

### 5.4 기본 점수 로직 (사용자가 바꿀 예정, O-1)
- **교체 지점 `MatchingScorer`** (`match/src/main/java/com/findear/match/scorer/`): 분실물과 후보(`MatchingSubject{key, category, color, productName, description}`)를 받아 원점수를 돌려준다. [0,1] 자르기·반올림·정렬·상한은 서비스가 공통으로 하고, NaN은 0으로 본다. `MatchingScorer`를 구현한 빈(`@Component` 또는 `@Bean`)을 하나 등록하면 기본 구현이 빠진다 (기본 구현은 자동 구성 `DefaultScorerAutoConfiguration`에서 `@ConditionalOnMissingBean`으로 등록 — 일반 `@Configuration`에서는 조건 평가 순서가 보장되지 않아서).
- 후보 대응: Findear 습득물 = key `acquiredBoardId`, category `categoryName`, color `color` / Lost112 = key `atcId`(비면 `id`), category `mainPrdtClNm`, color `clrNm`, productName `fdPrdtNm`, description `fdSbjt`. 분실물 key = 정수로 읽은 `lostBoardId`.
- 기본 구현 `DeterministicMatchingScorer`: `r = (v >>> 11) × 2^-53` (`v` = `SHA-256(UTF-8("<seed>|<lostBoardId>|<candidateKey>"))` 앞 8바이트 big-endian long), `raw = 0.3 + 0.6 × r` + 카테고리 일치 0.1 + 색상 일치 0.05 (양쪽 모두 비어 있지 않고 앞뒤 공백을 뺀 값이 같을 때).
- `/actuator/health`·`/actuator/prometheus`는 관리 포트(8085)에서만 제공 (호스트 비공개, D-21).
