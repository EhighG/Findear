#!/bin/sh
# Lost112 샘플 문서(police_acquired_data)를 Elasticsearch에 적재한다 (06 §8). 키 없이 목록·스크랩·매칭 화면을 확인하기 위한 가상 데이터다.
# - 전제: elasticsearch와 batch가 떠 있다 (인덱스와 매핑은 batch가 기동할 때 만든다).
# - 다시 실행해도 같은 결과다 (문서 _id = atcId, 날짜만 실행일 기준으로 다시 계산).
# - 환경변수: ES_URL (기본 http://localhost:${ES_HOST_PORT:-9200}), 필요한 것은 curl뿐이다.
# 사용: sh infra/elasticsearch/seed/load.sh
set -eu

DIR=$(cd "$(dirname "$0")" && pwd)
ES_URL=${ES_URL:-http://localhost:${ES_HOST_PORT:-9200}}
INDEX=police_acquired_data
DATA="$DIR/police_acquired_data.ndjson"

# n일 전 날짜(yyyy-MM-dd): GNU date(-d)가 없으면 BSD date(-v)를 쓴다
days_ago() {
  date -d "-$1 day" +%Y-%m-%d 2>/dev/null || date -v-"$1"d +%Y-%m-%d
}

status=$(curl -sS -o /dev/null -w '%{http_code}' -I "$ES_URL/$INDEX" || true)
if [ "$status" != "200" ]; then
  echo "오류: $ES_URL 에 $INDEX 인덱스가 없습니다 (HTTP $status). batch를 먼저 기동하세요 (인덱스·매핑은 batch가 만듭니다)." >&2
  exit 1
fi

# 파일의 __DAY0__(오늘), __DAY1__(어제) ... 을 날짜로 바꾼다
sed_script=""
n=0
while [ "$n" -le 30 ]; do
  sed_script="$sed_script -e s/__DAY${n}__/$(days_ago "$n")/g"
  n=$((n + 1))
done

# shellcheck disable=SC2086
response=$(sed $sed_script "$DATA" | curl -sS -X POST "$ES_URL/$INDEX/_bulk?refresh=true" \
  -H 'Content-Type: application/x-ndjson' --data-binary @-)

case "$response" in
  *'"errors":false'*) ;;
  *)
    echo "오류: bulk 적재 중 실패한 항목이 있습니다." >&2
    echo "$response" | head -c 2000 >&2
    echo >&2
    exit 1
    ;;
esac

created=$(printf '%s' "$response" | grep -o '"result":"created"' | wc -l | tr -d ' ')
updated=$(printf '%s' "$response" | grep -o '"result":"updated"' | wc -l | tr -d ' ')
total=$(curl -sS "$ES_URL/$INDEX/_count" | grep -o '"count":[0-9]*' | head -n 1 | cut -d: -f2)
echo "적재 완료: 샘플 $((created + updated))건 (신규 $created, 덮어씀 $updated). $INDEX 전체 문서 ${total}건"
