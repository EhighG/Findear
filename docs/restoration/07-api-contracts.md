# 07. 서버 간 API 계약

> 공통 응답 형식(main·batch): 성공 `{"status": int, "message": str, "result": any}`, 실패 `{"status": int, "message": str}`.
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
| 분실물 등록 직후 매칭 | `POST {batch}/findear/matching` | `{lostBoardId, productName, color, categoryName, description, lostAt, xpos, ypos}` | `{findearDatas: [{lostBoardId, acquiredBoardId, similarityRate}], policeDatas: [...]}` — main은 `findearDatas[0].lostBoardId`로 FCM 알림 발송 |
| 매칭 목록 | `GET {batch}/{findear\|police}/{member\|board}/{id}?page=1&size=6` | – | `{matchingList: [...], totalCount}` → main이 `totalPageNum`으로 변환 |

- findear 매칭 항목: `{findearMatchingLogId, lostBoardId, acquiredBoardId, similarityRate, matchedAt}`
- police 매칭 항목: `{policeMatchingLogId, lostBoardId, similarityRate, matchedAt, acquiredBoardId, atcId, depPlace, fdFilePathImg, fdPrdtNm, fdSbjt, clrNm, fdYmd, mainPrdtClNm}`
- **K-01** (해결, R-22): master main은 Lost112 목록을 `{batch}?page=`, 총개수를 `{batch}/total`로 호출했음 → `/search`, `/search/total`로 수정, 쿼리는 `UriComponentsBuilder`로 한 번만 인코딩. 남은 점: `keyword`의 `+`는 인코딩되지 않아 batch가 공백으로 읽을 수 있음(수정 전에도 같음, R-35에서 확인).
- R-22에서 main의 나머지 호출 경로(`/police/scrap`, `/findear/matching`, 매칭 목록, match `/process`)가 이 표와 같음을 확인. 매칭 요청 DTO의 `xPos`/`yPos`가 JSON에서 `xpos`/`ypos`로 나가는지는 R-35에서 확인.

## 3. batch API 전체 (팀 버전)와 1차 처리

| 메서드·경로 | 용도 | 처리 |
|---|---|---|
| `POST /findear/matching` | 분실물 1건 매칭 (main 호출) | 유지 |
| `POST /findear/matching/batch` | 전체 분실물 매칭 수동 실행 | 유지 (내부용) |
| `GET /findear/member/{memberId}`, `GET /findear/board/{lostBoardId}` | Findear 매칭 목록 | 유지 |
| `GET /police/member/{memberId}`, `GET /police/board/{lostBoardId}` | Lost112 매칭 목록 | 유지 |
| `POST /police/scrap` | 스크랩 습득물 조회 | 유지 |
| `GET /search`, `GET /search/total` | Lost112 목록·총개수 | 유지 |
| `POST /search/save` | Lost112 수집 수동 실행 | 유지 (내부용) |
| `GET /findear`, `GET /search/all`, `GET /search/save?page&size`(이름과 달리 조회) | 전체 조회 | local 프로필 한정 |
| `DELETE /findear`, `DELETE /police`, `DELETE /search` | 전체 삭제 | local 프로필 한정 |
| `GET /findear/test-api`, `GET /police/test-api`, `GET /search/test` | 테스트 | 삭제 또는 local 한정 |

batch는 호스트/외부에 공개하지 않습니다(로컬은 127.0.0.1 디버깅용만).

## 4. main 외부 API 목록 (47개, 테스트·프론트 복구 참고)

| base | 엔드포인트 |
|---|---|
| `/members` | `POST /`(가입·local), `PATCH /{id}/role`, `POST /login`(전화번호 로그인·local), `GET /login`(Naver 콜백), `GET /after-login?code`, `POST /logout`, `PATCH /{id}`, `PATCH /{id}/delete`, `POST /token/refresh`, `POST /duplicate`, `GET /{id}`, `GET /token-check`, `GET /` |
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

### 5.1 `POST /process` (main → match, 습득물 자동채움)
- 요청 `{productName, imgUrl}` → 응답 200 `{"message":"success","result":{"category": str,"color": str,"description":[str×5]}}`
- 카테고리 후보: 카드, 지갑, 현금, 의류, 전자기기, 가방, 휴대폰, 증명서, 쇼핑백, 귀금속, 유가증권, 자동차, 서류, 도서용품, 스포츠용품, 컴퓨터, 산업용품, 악기, 기타
- 색상 후보: 검정, 흰, 빨강, 오렌지, 노랑, 초록, 파랑, 갈, 보라, 회, 기타
- 기본 구현: `hash(productName + imgUrl + MATCH_MOCK_SEED)`로 결정적 선택, 키워드 5개는 고정 풀 + productName 토큰.
- 잘못된 요청 400, 실패 흉내가 필요하면 404 `{"message":"GPT api failed"}` (팀 계약).

### 5.2 `POST /matching/findear` (batch → match)
- 요청 `{"lostBoard": {lostBoardId, productName, color, categoryName, description, lostAt, xpos, ypos}, "acquiredBoardList": [{acquiredBoardId, productName, color, categoryName, description, xpos, ypos, registeredAt}]}` — **값이 전부 문자열**로 옴(batch DTO가 String).
- 응답 200 `{"message": str, "result": [{"lostBoardId": int, "acquiredBoardId": int, "similarityRate": float}]}` — 점수 내림차순, 최대 `MATCH_MOCK_MAX_RESULTS`(100)개, 소수 5자리.
- `acquiredBoardList`가 비면 200 `{"message": "...없습니다.", "result": null}`.

### 5.3 `POST /matching/lost` (batch → match, Lost112 매칭)
- 요청 `{"lostBoard": {...위와 동일}, "acquiredBoardList": [{id, atcId, depPlace, fdFilePathImg, fdPrdtNm, fdSbjt, clrNm, fdYmd, mainPrdtClNm}]}`
- 응답 `result`: `[{lostBoardId, acquiredBoardId(=int(id)), similarityRate, atcId, depPlace, fdFilePathImg, fdPrdtNm, fdSbjt, clrNm, fdYmd, mainPrdtClNm}]` (입력 필드를 그대로 되돌려줌), 정렬·상한·빈 목록 처리는 5.2와 동일.

### 5.4 기본 점수 로직 (사용자가 바꿀 예정, O-1)
- `MatchingScorer` 인터페이스로 분리해 교체 가능하게.
- 기본: `base = 0.3 + 0.6 × rand(seed, lostBoardId, candidateId)` (결정적 의사난수) + 카테고리 일치 시 +0.1 (Lost112는 `mainPrdtClNm` 비교) + 색상 일치 시 +0.05 → [0,1]로 자르기.
- `MATCH_MOCK_LATENCY_MS`만큼 지연 가능 (모니터링 확인용).
- `/actuator/health`는 관리 포트(8085)에서 제공.
