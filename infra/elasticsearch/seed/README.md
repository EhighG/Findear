# Lost112 샘플 문서 적재

공공데이터포털 키(`LOST112_SERVICE_KEY`) 없이 Lost112 습득물 목록·스크랩·매칭 화면을 확인하기 위한 **가상** 문서 16건입니다.
실제 수집 데이터가 아닙니다 (관리 ID는 `F20991001…` 형태의 가짜이고 기관 이름 앞에 "가상"이 붙어 있습니다).
키가 있으면 batch가 직접 수집합니다 (`POST {batch}/search/save`, 05 §3).

## 구성

| 파일 | 내용 |
|---|---|
| `police_acquired_data.ndjson` | Elasticsearch bulk 형식. 문서 `_id` = `atcId`, 필드는 batch 수집이 만드는 정규화된 형식 (`fdYmd` `yyyy-MM-dd`, `mainPrdtClNm`·`subPrdtClNm`, `source` 등). `fdFilePathImg`는 비어 있음(사진 없음), 일부는 `clrNm`이 없음 |
| `load.sh` | 날짜 자리표시자를 바꿔 `_bulk`로 적재하는 POSIX sh 스크립트 (curl만 필요) |

날짜는 파일에 `__DAY0__`(오늘), `__DAY1__`(어제) … 로 들어 있고 `load.sh`가 실행하는 날 기준 `yyyy-MM-dd`로 바꿉니다.
그래서 개발용 시드 분실물(지갑: 오늘-3일, 전자기기: 오늘-2일 분실, `infra/db/seed/R__dev_seed.sql`)과 매칭 후보(같은 카테고리, 습득일 ≥ 분실일)가 항상 생깁니다.
카테고리: 지갑 5, 전자기기 4, 가방 2, 휴대폰, 의류, 현금, 카드, 서류 각 1건.

## 실행

전제: `elasticsearch`와 `batch`가 떠 있어야 합니다. 인덱스와 매핑(`fdYmd` date, `atcId` keyword 등)은 batch가 기동할 때 만들고, 이 스크립트는 인덱스가 없으면 실패합니다.

```sh
docker compose up -d --build mysql flyway elasticsearch batch   # batch가 healthy가 될 때까지 기다린다
sh infra/elasticsearch/seed/load.sh
```

- 기본 접속 주소는 `http://localhost:${ES_HOST_PORT:-9200}`입니다. 다르면 `ES_URL=http://host:port sh infra/elasticsearch/seed/load.sh`.
- 다시 실행해도 같은 결과입니다 (같은 `_id`를 덮어쓰고 날짜만 그날 기준으로 다시 계산).
- 확인: `curl localhost:8082/search/total` (로컬 `.env`의 `BATCH_HOST_PORT`를 따른다), `curl 'localhost:8082/search?page=1&size=5'`.
- 이 PC처럼 호스트 포트를 바꿨으면(`.env`의 `ES_HOST_PORT`) `ES_HOST_PORT`를 환경변수로 같이 준다.

## 지우기

샘플만 지우려면:

```sh
curl -X POST "http://localhost:${ES_HOST_PORT:-9200}/police_acquired_data/_delete_by_query?refresh=true" \
  -H 'Content-Type: application/json' -d '{"query":{"prefix":{"atcId":"F20991001"}}}'
```

인덱스째 지우려면 `docker compose down -v`(로컬 볼륨 전체 삭제) 또는 `curl -X DELETE http://localhost:${ES_HOST_PORT:-9200}/police_acquired_data` 후 batch를 재기동합니다
(batch가 인덱스를 다시 만듭니다). 이미 있는 인덱스의 매핑은 Spring Data가 바꾸지 않으므로, 매핑을 바꾼 뒤에는 인덱스를 지우고 batch를 재기동해야 합니다.

## 주의

- 샘플의 `fdFilePathImg`가 빈 문자열인 것은 batch의 Lost112 매칭이 사진 URL이 `null`이면 실패하는 현재 동작(R-35에서 고침) 때문이기도 합니다. 실제 수집 데이터에서 사진이 없는 항목은 필드가 없거나 `null`입니다.
- 샘플과 실제 수집 문서가 함께 있으면 둘 다 목록에 나옵니다. 가짜 문서는 위 삭제 쿼리로 지웁니다.
