#!/usr/bin/env bash
# 외부 연동 확인 스크립트 (R-81). 키를 세팅한 뒤(U-04·U-05·U-07) 스택을 띄워 놓고 R-91에서 사용자가 실행한다.
#
#   tools/verify-external/verify.sh                         # 설정된 연동만 확인
#   tools/verify-external/verify.sh --only vworld           # 확인할 연동을 좁힘 (vworld|lost112|fcm, 여러 번 가능)
#   tools/verify-external/verify.sh --skip-lost112-collect  # Lost112는 설정 검사만 (수집 요청을 보내지 않음)
#   tools/verify-external/verify.sh --fcm-phone 010-0000-0001   # FCM 테스트 발송까지 (local 프로필의 시드 회원)
#   tools/verify-external/verify.sh --yes                   # 확인 프롬프트 없이 진행
#
# 동작
#   1단계 설정 검사: .env·secrets/·tools/fcm-test/firebase-config.js의 누락·오류를 본다. 네트워크 요청 없음.
#   2단계 확인 요청: 1단계가 "설정됨"인 연동만 main·batch(localhost)의 엔드포인트로 요청한다.
#     확인할 연동이 하나도 없으면 어떤 HTTP 요청도 보내지 않는다 (main·batch 헬스 확인도 하지 않는다).
#   3단계 요약: 연동 | 상태 | 메모 표. 설정 오류·확인 실패가 하나라도 있으면 종료 코드 1 (미설정만 있으면 0).
#
# 외부 호출이 일어나는 때: 2단계에서만, 그리고 main·batch가 대신 부른다 (이 스크립트가 외부 서비스에 직접 요청하지 않는다).
#   VWorld  main이 api.vworld.kr를 호출한다.
#   Lost112 batch가 apis.data.go.kr를 호출한다 (최근 LOST112_COLLECT_DAYS일 전체 수집, 페이지마다 1회 호출, 포털기관 개발계정 일일 호출 한도 10,000회에서 차감).
#   FCM     main이 FCM을 호출한다 (테스트 발송, --fcm-phone을 줬을 때만).
# 호출 전에 무엇을 부르는지 출력하고, --yes가 없으면 [y/N]로 묻는다 (터미널이 아니면 묻지 않고 요청을 보내지 않는다).
#
# 설정은 .env 파일 기준이다. 셸 환경변수가 .env보다 우선하는 Compose 규칙은 다루지 않는다.
# 키·JWT·서비스계정 내용·firebase-config.js 내용은 출력하지 않는다.
# 참고: docs/restoration/05-external-integrations.md §9 (키 세팅 체크리스트), tools/verify-external/README.md
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

usage() {
  cat <<'EOF'
사용: tools/verify-external/verify.sh [옵션]

  --only <연동>            확인 요청을 보낼 연동을 좁힌다: vworld | lost112 | fcm (여러 번 줄 수 있음)
  --skip-lost112-collect   Lost112는 설정 검사만 하고 수집 요청(POST /search/save)을 보내지 않는다
  --fcm-phone <전화번호>   FCM 테스트 발송까지 한다 (main이 local 프로필이고 시드 회원이어야 함. 예: 010-0000-0001)
  --yes                    확인 프롬프트 없이 진행한다
  -h, --help               이 도움말

1단계(설정 검사)는 네트워크를 쓰지 않는다. 설정된 연동이 없으면 어떤 요청도 보내지 않고 요약만 출력한다.
설정은 .env 기준이다. 자세한 설명: tools/verify-external/README.md
EOF
}

say() { printf '%s\n' "$*"; }

# --- .env 읽기 -------------------------------------------------------------------------------------------------
# 아래 env_get은 infra/deploy/deploy.sh의 env_get을 그대로 복사한 것이다 (.env를 source하지 않는 Compose .env 규칙 파서).
# 원본을 고치면 여기도 맞춘다.
#
# KEY=값 줄에서 마지막 값을 꺼낸다 (Compose .env 규칙을 따르는 최소 구현).
# Compose .env 문법: https://docs.docker.com/compose/how-tos/environment-variables/variable-interpolation/ (".env file syntax")
#   - 값 앞뒤 공백은 무시한다. 따옴표(' ")로 감싼 값은 여는 따옴표부터 같은 종류의 닫는 따옴표까지가 값이고,
#     닫는 따옴표 뒤의 " # 주석"은 값이 아니다. 따옴표 없는 값은 " #"(공백 뒤 #) 앞까지가 값이다.
#   - 줄 앞의 "export " 접두사와 "KEY = 값"의 = 앞뒤 공백도 받아들인다.
#   - 닫는 따옴표가 없으면 따옴표 없는 값처럼 그대로 둔다.
#   - 하지 않는 것: 큰따옴표 안 이스케이프(\")·변수 치환($VAR), "KEY: 값" 구분자. 검사 대상 값에는 $·\ 를 이미 금지한다.
# 파일에 KEY가 없으면 빈 문자열.
env_get() {
  local file="$1" key="$2" line value="" rest tail quoted
  while IFS= read -r line || [ -n "$line" ]; do
    line="${line%$'\r'}"
    line="${line#"${line%%[![:space:]]*}"}"   # 앞 공백
    case "$line" in
      export[[:space:]]*) line="${line#export}"; line="${line#"${line%%[![:space:]]*}"}" ;;
    esac
    case "$line" in
      "$key"*)
        rest="${line#"$key"}"
        rest="${rest#"${rest%%[![:space:]]*}"}"   # KEY 뒤 공백
        case "$rest" in
          =*) value="${rest#=}" ;;
        esac ;;
    esac
  done < "$file"
  # 값 앞 공백 제거
  value="${value#"${value%%[![:space:]]*}"}"
  quoted=0
  case "$value" in
    \"*)
      tail="${value#\"}"
      if [[ "$tail" == *\"* ]]; then value="${tail%%\"*}"; quoted=1; fi ;;
    \'*)
      tail="${value#\'}"
      if [[ "$tail" == *\'* ]]; then value="${tail%%\'*}"; quoted=1; fi ;;
  esac
  if [ "$quoted" -eq 0 ]; then
    # 따옴표 없는 값(또는 닫는 따옴표 없음): 공백 뒤 #은 주석
    value="${value%% \#*}"
    value="${value%"${value##*[![:space:]]}"}"
  fi
  printf '%s' "$value"
}

trim() {
  local s="$1"
  s="${s#"${s%%[![:space:]]*}"}"
  s="${s%"${s##*[![:space:]]}"}"
  printf '%s' "$s"
}

lower() { printf '%s' "$1" | tr '[:upper:]' '[:lower:]'; }

# Spring의 boolean 변환과 같게: true/yes/on/1 → true, false/no/off/0/빈 값 → false, 그 밖은 invalid (05 §2 @ConditionalOnFcm)
parse_bool() {
  local v
  v="$(lower "$(trim "$1")")"
  case "$v" in
    true|yes|on|1) printf 'true' ;;
    false|no|off|0|"") printf 'false' ;;
    *) printf 'invalid' ;;
  esac
}

# --- 연동별 상태 ------------------------------------------------------------------------------------------------
# 0 VWorld, 1 Lost112, 2 FCM 서버, 3 FCM 테스트 페이지, 4 Naver (bash 3.2에서도 되도록 인덱스 배열만 쓴다)
I_VW=0; I_LOST=1; I_FCMS=2; I_FCMP=3; I_NAVER=4
NAMES=("VWorld" "Lost112" "FCM 서버" "FCM 테스트 페이지" "Naver 로그인")
STATUS=("" "" "" "" "")
MEMO=("" "" "" "" "")

set_row() { STATUS[$1]="$2"; MEMO[$1]="${3:-}"; }
add_memo() {
  if [ -n "${MEMO[$1]}" ]; then MEMO[$1]="${MEMO[$1]}; $2"; else MEMO[$1]="$2"; fi
}
show_row() {
  local m="${MEMO[$1]}"
  say "  - ${NAMES[$1]}: ${STATUS[$1]}${m:+ -- $m}"
}

# --- HTTP ------------------------------------------------------------------------------------------------------
# 모든 HTTP 요청은 이 함수 하나로 curl을 부른다. 결과는 HTTP_CODE(연결 실패면 000), HTTP_BODY, HTTP_RC(curl 종료 코드).
#   http_call <METHOD> <URL> <max-time초> [curl 추가 인자...]
ERR_FILE=""
HTTP_CODE="000"; HTTP_BODY=""; HTTP_RC=0
http_call() {
  local method="$1" url="$2" max="$3" out rc=0
  shift 3
  HTTP_CODE="000"; HTTP_BODY=""
  out="$(curl -sS -X "$method" --connect-timeout 5 --max-time "$max" -w $'\n%{http_code}' "$@" "$url" 2> "$ERR_FILE")" || rc=$?
  HTTP_RC=$rc
  if [ "$rc" -eq 0 ]; then
    HTTP_CODE="${out##*$'\n'}"
    HTTP_BODY="${out%$'\n'*}"
  fi
}

# JSON 파서 없이(jq·python 없음) 필요한 필드만 grep/sed로 꺼낸다. 같은 이름이 여럿이면 첫 번째.
json_str() { # <본문> <키> : "키": "문자열 값"
  { printf '%s' "$1" | tr -d '\r\n' | { grep -o "\"$2\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" || true; } | head -n 1 \
      | sed 's/^[^:]*:[[:space:]]*"\(.*\)"$/\1/'; } || true
}
json_num() { # <본문> <키> : "키": 숫자
  { printf '%s' "$1" | tr -d '\r\n' | { grep -o "\"$2\"[[:space:]]*:[[:space:]]*[0-9][0-9]*" || true; } | head -n 1 \
      | sed 's/^[^:]*:[[:space:]]*//'; } || true
}
clip() { # <문자열> [길이] : 줄바꿈 제거 후 자른다
  local s="${1//$'\r'/}" n="${2:-200}"
  s="${s//$'\n'/ }"
  if [ "${#s}" -gt "$n" ]; then printf '%s...' "${s:0:n}"; else printf '%s' "$s"; fi
}

# 요청 한 줄 로그. 쿼리 값에는 비밀값이 없다 (키는 main·batch가 붙인다)
show_req() { say "    $1 $2 -> HTTP $HTTP_CODE"; }

# curl 자체가 실패했을 때의 설명 (main|batch)
conn_fail_msg() {
  local who="$1"
  if [ "$HTTP_RC" -eq 28 ]; then
    printf '%s 요청 시간 초과(curl 종료 코드 28)' "$who"
  else
    printf '%s 요청 실패(curl 종료 코드 %s): %s 컨테이너가 떠 있지 않나요? docker compose up -d --build %s' "$who" "$HTTP_RC" "$who" "$who"
  fi
}

# --- 옵션 ------------------------------------------------------------------------------------------------------
SEL_VW=1; SEL_LOST=1; SEL_FCM=1; ONLY_SET=0
SKIP_LOST_COLLECT=0
FCM_PHONE=""
ASSUME_YES=0

while [ $# -gt 0 ]; do
  case "$1" in
    --only)
      [ $# -ge 2 ] || { say "오류: --only 값이 필요합니다 (vworld|lost112|fcm)" >&2; exit 2; }
      if [ "$ONLY_SET" -eq 0 ]; then ONLY_SET=1; SEL_VW=0; SEL_LOST=0; SEL_FCM=0; fi
      case "$2" in
        vworld)  SEL_VW=1 ;;
        lost112) SEL_LOST=1 ;;
        fcm)     SEL_FCM=1 ;;
        *) say "오류: --only 값은 vworld, lost112, fcm 중 하나여야 합니다: $2" >&2; exit 2 ;;
      esac
      shift 2 ;;
    --skip-lost112-collect) SKIP_LOST_COLLECT=1; shift ;;
    --fcm-phone)
      [ $# -ge 2 ] || { say "오류: --fcm-phone 값이 필요합니다 (예: 010-0000-0001)" >&2; exit 2; }
      FCM_PHONE="$2"
      if ! [[ "$FCM_PHONE" =~ ^[0-9+-]+$ ]]; then
        say "오류: --fcm-phone은 숫자와 -, +만 쓸 수 있습니다" >&2; exit 2
      fi
      shift 2 ;;
    --yes) ASSUME_YES=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) say "오류: 알 수 없는 옵션: $1" >&2; usage >&2; exit 2 ;;
  esac
done

ENV_FILE="$ROOT/.env"
if [ ! -f "$ENV_FILE" ]; then
  say "오류: .env가 없습니다: $ENV_FILE" >&2
  say "      cp .env.example .env 로 만든 뒤 키를 채우세요 (docs/restoration/05-external-integrations.md §9)." >&2
  exit 2
fi

MAIN_PORT="$(trim "$(env_get "$ENV_FILE" MAIN_HOST_PORT)")"; MAIN_PORT="${MAIN_PORT:-8080}"
BATCH_PORT="$(trim "$(env_get "$ENV_FILE" BATCH_HOST_PORT)")"; BATCH_PORT="${BATCH_PORT:-8082}"
for p in "$MAIN_PORT" "$BATCH_PORT"; do
  if ! [[ "$p" =~ ^[0-9]+$ ]]; then
    say "오류: .env의 MAIN_HOST_PORT·BATCH_HOST_PORT는 숫자여야 합니다: $p" >&2
    exit 2
  fi
done
MAIN_URL="http://localhost:${MAIN_PORT}"
BATCH_URL="http://localhost:${BATCH_PORT}"

ERR_FILE="$(mktemp)"
trap 'rm -f "$ERR_FILE"' EXIT

# --- 1단계: 설정 검사 (네트워크 없음) -------------------------------------------------------------------------------
say "== 1단계: 설정 검사 (.env 기준, 네트워크 요청 없음) =="

# VWorld
vw_key="$(trim "$(env_get "$ENV_FILE" VWORLD_API_KEY)")"
if [ -z "$vw_key" ]; then
  set_row $I_VW "미설정" "VWORLD_API_KEY가 비어 있음 (U-07)"
else
  set_row $I_VW "설정됨" "VWORLD_API_KEY 설정됨(길이 ${#vw_key})"
fi

# Lost112
lost_key="$(trim "$(env_get "$ENV_FILE" LOST112_SERVICE_KEY)")"
lost_collect_raw="$(env_get "$ENV_FILE" LOST112_COLLECT_ENABLED)"
lost_collect="$(parse_bool "$lost_collect_raw")"
lost_days="$(trim "$(env_get "$ENV_FILE" LOST112_COLLECT_DAYS)")"; lost_days="${lost_days:-30}"
lost_page="$(trim "$(env_get "$ENV_FILE" LOST112_PAGE_SIZE)")"; lost_page="${lost_page:-1000}"
if [ "$lost_collect" = "invalid" ]; then
  set_row $I_LOST "설정 오류" "LOST112_COLLECT_ENABLED 값이 true/false가 아님 (batch가 기동하지 않을 수 있음)"
elif [ -z "$lost_key" ]; then
  if [ "$lost_collect" = "true" ]; then
    set_row $I_LOST "설정 오류" "LOST112_COLLECT_ENABLED=true인데 LOST112_SERVICE_KEY가 비어 있음 (켜도 수집되지 않음, U-05)"
  else
    set_row $I_LOST "미설정" "LOST112_SERVICE_KEY가 비어 있음 (U-05)"
  fi
else
  set_row $I_LOST "설정됨" "LOST112_SERVICE_KEY 설정됨(길이 ${#lost_key})"
  if [ "$lost_collect" = "true" ]; then
    add_memo $I_LOST "정기 수집 켜짐(LOST112_COLLECT_ENABLED=true)"
  else
    add_memo $I_LOST "정기 수집 꺼짐(수동 수집만)"
  fi
fi

# FCM 서버
fcm_raw="$(env_get "$ENV_FILE" FCM_ENABLED)"
fcm_enabled="$(parse_bool "$fcm_raw")"
fcm_path="$(trim "$(env_get "$ENV_FILE" FCM_CREDENTIALS_PATH)")"
fcm_path="${fcm_path:-/run/secrets/firebase-adminsdk.json}"
fcm_host_file=""
case "$fcm_path" in
  /run/secrets/*)
    case "$fcm_path" in
      */../*|*/..) fcm_host_file="" ;;
      *) fcm_host_file="$ROOT/secrets/${fcm_path#/run/secrets/}" ;;
    esac ;;
esac
if [ "$fcm_enabled" = "invalid" ]; then
  set_row $I_FCMS "설정 오류" "FCM_ENABLED 값이 true/false가 아님 (main이 기동하지 않습니다)"
elif [ "$fcm_enabled" = "false" ]; then
  set_row $I_FCMS "미설정" "FCM_ENABLED가 꺼져 있음 (U-04)"
  if [ -n "$fcm_host_file" ] && [ -f "$fcm_host_file" ]; then
    add_memo $I_FCMS "서비스계정 파일은 이미 있음: FCM_ENABLED=true로 바꾸면 켜짐"
  fi
else
  if [ -z "$fcm_host_file" ]; then
    set_row $I_FCMS "설정 오류" "FCM_CREDENTIALS_PATH가 /run/secrets/ 아래의 경로가 아님 (./secrets만 마운트됨, main이 기동하지 않습니다)"
  elif [ -d "$fcm_host_file" ]; then
    set_row $I_FCMS "설정 오류" "secrets/${fcm_path#/run/secrets/}가 디렉토리임 (main이 기동하지 않습니다)"
  elif [ ! -f "$fcm_host_file" ]; then
    set_row $I_FCMS "설정 오류" "FCM_ENABLED=true인데 secrets/${fcm_path#/run/secrets/}가 없음 (main이 기동하지 않습니다)"
  elif [ ! -s "$fcm_host_file" ]; then
    set_row $I_FCMS "설정 오류" "secrets/${fcm_path#/run/secrets/}가 비어 있음 (main이 기동하지 않습니다)"
  elif ! grep -q '"type"[[:space:]]*:[[:space:]]*"service_account"' "$fcm_host_file"; then
    set_row $I_FCMS "설정 오류" "secrets/${fcm_path#/run/secrets/}가 서비스계정 JSON이 아님 (\"type\": \"service_account\" 없음)"
  else
    set_row $I_FCMS "설정됨" "FCM_ENABLED=true, 서비스계정 파일 확인(내용은 출력하지 않음)"
  fi
fi

# FCM 테스트 페이지 (tools/fcm-test/firebase-config.js): 필수 값(page가 요구하는 5개)이 비었거나 없으면 설정 오류
fcm_cfg="$ROOT/tools/fcm-test/firebase-config.js"
if [ ! -f "$fcm_cfg" ]; then
  set_row $I_FCMP "미설정" "tools/fcm-test/firebase-config.js 없음 (firebase-config.example.js를 복사해 채움, U-04)"
else
  cfg_body="$(grep -vE '^[[:space:]]*//' "$fcm_cfg" || true)"
  missing=""
  for f in apiKey projectId messagingSenderId appId vapidKey; do
    # 공백만 든 값도 빈 값으로 본다 (페이지 app.js의 isBlank와 같게)
    if ! printf '%s\n' "$cfg_body" | grep -qE "[\"']?${f}[\"']?[[:space:]]*:[[:space:]]*[\"'][[:space:]]*[^\"'[:space:]][^\"']*[\"']"; then
      missing="${missing:+$missing, }$f"
    fi
  done
  opt_empty=""
  for f in authDomain storageBucket; do
    if printf '%s\n' "$cfg_body" | grep -qE "[\"']?${f}[\"']?[[:space:]]*:[[:space:]]*(\"\"|'')"; then
      opt_empty="${opt_empty:+$opt_empty, }$f"
    fi
  done
  if [ -n "$missing" ]; then
    set_row $I_FCMP "설정 오류" "채우지 않은 필수 값: $missing (내용은 출력하지 않음)"
  else
    set_row $I_FCMP "설정됨" "필수 값 5개가 채워져 있음(내용은 출력하지 않음)"
    if [ -n "$opt_empty" ]; then add_memo $I_FCMP "선택 값이 비어 있음: $opt_empty"; fi
  fi
fi

# Naver: 1차 범위 밖 (D-50). 값이 있어도 확인하지 않는다
naver_id="$(trim "$(env_get "$ENV_FILE" NAVER_CLIENT_ID)")"
naver_secret="$(trim "$(env_get "$ENV_FILE" NAVER_CLIENT_SECRET)")"
if [ -n "$naver_id" ] || [ -n "$naver_secret" ]; then
  set_row $I_NAVER "제외(추후, D-50)" "값이 있지만 1차 범위 밖이라 확인하지 않음"
else
  set_row $I_NAVER "제외(추후, D-50)" "Naver 로그인은 추후 진행 (05 §4)"
fi

for i in 0 1 2 3 4; do show_row $i; done

# --- 2단계: 확인 요청 ------------------------------------------------------------------------------------------------
say ""
say "== 2단계: 확인 요청 =="

T_VW=0; T_LOST=0; T_FCM=0
if [ "${STATUS[$I_VW]}" = "설정됨" ]; then
  if [ "$SEL_VW" -eq 0 ]; then set_row $I_VW "확인 안 함" "--only로 제외됨"; else T_VW=1; fi
fi
if [ "${STATUS[$I_LOST]}" = "설정됨" ]; then
  if [ "$SEL_LOST" -eq 0 ]; then
    set_row $I_LOST "확인 안 함" "--only로 제외됨"
  elif [ "$SKIP_LOST_COLLECT" -eq 1 ]; then
    set_row $I_LOST "건너뜀" "--skip-lost112-collect: 수집 요청을 보내지 않음 (키 설정만 확인)"
  else
    T_LOST=1
  fi
fi
if [ "${STATUS[$I_FCMS]}" = "설정됨" ]; then
  if [ "$SEL_FCM" -eq 0 ]; then
    set_row $I_FCMS "확인 안 함" "--only로 제외됨"
  elif [ -z "$FCM_PHONE" ]; then
    set_row $I_FCMS "안내" "테스트 발송은 하지 않음: tools/fcm-test 페이지에서 토큰 등록 후 --fcm-phone <전화번호>로 다시 실행"
  else
    T_FCM=1
  fi
elif [ -n "$FCM_PHONE" ] && [ "$SEL_FCM" -eq 1 ]; then
  add_memo $I_FCMS "--fcm-phone은 FCM 서버가 설정됨일 때만 쓰임(무시됨)"
fi

if [ $((T_VW + T_LOST + T_FCM)) -eq 0 ]; then
  say "확인 요청을 보낼 연동이 없습니다. 어떤 HTTP 요청도 보내지 않았습니다 (main·batch 헬스 확인 포함)."
  say "키 세팅 방법: docs/restoration/05-external-integrations.md §9"
else
  say "다음 요청을 보냅니다 (main ${MAIN_URL}, batch ${BATCH_URL}):"
  if [ $T_VW -eq 1 ]; then
    say "  - VWorld : main GET /location/search, /location/address -> main이 VWorld(api.vworld.kr)를 호출합니다 (외부로 나가는 요청)"
  fi
  if [ $T_LOST -eq 1 ]; then
    say "  - Lost112: batch POST /search/save -> batch가 공공데이터포털(apis.data.go.kr)에서 최근 ${lost_days}일 전체를 페이지(${lost_page}건)마다 받습니다."
    say "             페이지마다 1회씩 포털기관 개발계정 일일 호출 한도(10,000회)를 쓰고 수분 걸릴 수 있습니다 (건너뛰려면 --skip-lost112-collect)"
  fi
  if [ $T_FCM -eq 1 ]; then
    say "  - FCM    : main POST /members/login, POST /alarm/send-fcm/{memberId} -> main이 FCM을 호출합니다 (외부로 나가는 요청)"
  fi

  proceed=1
  if [ "$ASSUME_YES" -eq 0 ]; then
    if [ -t 0 ]; then
      printf '계속할까요? [y/N] '
      ans=""
      read -r ans || ans=""
      case "$ans" in
        y|Y|yes|YES) proceed=1 ;;
        *) proceed=0; say "취소했습니다. 요청을 보내지 않았습니다." ;;
      esac
    else
      proceed=0
      say "터미널이 아니라 확인할 수 없어 요청을 보내지 않았습니다. 진행하려면 --yes를 붙여 다시 실행하세요."
    fi
  fi

  if [ "$proceed" -eq 0 ]; then
    if [ $T_VW -eq 1 ]; then set_row $I_VW "확인 안 함" "요청을 보내지 않음(취소했거나 비대화형: --yes로 진행)"; fi
    if [ $T_LOST -eq 1 ]; then set_row $I_LOST "확인 안 함" "요청을 보내지 않음(취소했거나 비대화형: --yes로 진행)"; fi
    if [ $T_FCM -eq 1 ]; then set_row $I_FCMS "확인 안 함" "요청을 보내지 않음(취소했거나 비대화형: --yes로 진행)"; fi
    T_VW=0; T_LOST=0; T_FCM=0
  fi
fi

# VWorld: /location/search, /location/address (VWorld 응답은 본문 그대로 200으로 전달되므로 response.status로 판단)
VW_NOTE=""; VW_FAIL=""
vworld_call() { # <라벨> <경로>
  local label="$1" path="$2" st code msg
  http_call GET "${MAIN_URL}${path}" 30
  show_req GET "${path%%\?*}"
  if [ "$HTTP_RC" -ne 0 ]; then
    VW_FAIL="$(conn_fail_msg main)"
    return 1
  fi
  case "$HTTP_CODE" in
    200)
      st="$(json_str "$HTTP_BODY" status)"
      case "$st" in
        OK) VW_NOTE="${VW_NOTE:+$VW_NOTE, }$label OK" ;;
        NOT_FOUND) VW_NOTE="${VW_NOTE:+$VW_NOTE, }$label NOT_FOUND(키는 동작, 결과 없음)" ;;
        ERROR)
          code="$(json_str "$HTTP_BODY" code)"
          VW_FAIL="$label: VWorld 오류 응답 error.code=${code:-?} (INVALID_KEY·INCORRECT_KEY·UNAVAILABLE_KEY 등은 05 §5·§8 참고: 키·등록한 서비스 URL 확인)"
          return 1 ;;
        *)
          VW_FAIL="$label: 응답을 해석하지 못함(response.status 없음): $(clip "$HTTP_BODY" 120)"
          return 1 ;;
      esac ;;
    503)
      VW_FAIL="$label: main이 VWORLD_API_KEY를 못 읽음(503) -> .env 확인 후 docker compose up -d main 으로 컨테이너를 다시 만드세요"
      return 1 ;;
    502)
      msg="$(json_str "$HTTP_BODY" message)"
      VW_FAIL="$label: main이 VWorld 호출에 실패함(502): $(clip "${msg:-$HTTP_BODY}" 150) (네트워크·VWorld 장애, 05 §5 참고)"
      return 1 ;;
    *)
      msg="$(json_str "$HTTP_BODY" message)"
      VW_FAIL="$label: 예상 밖 응답 HTTP $HTTP_CODE: $(clip "${msg:-$HTTP_BODY}" 150)"
      return 1 ;;
  esac
  return 0
}

verify_vworld() {
  say "-- VWorld"
  # 쿼리 값은 미리 퍼센트 인코딩한 상수로 둔다. 셸에서 바이트 단위로 인코딩하면 bash 3.2(macOS 기본)·musl bash에서
  # UTF-8 바이트가 부호 있는 값으로 바뀌어 틀어진다 (R-81 검증에서 확인).
  local q='%EC%84%9C%EC%9A%B8%EC%97%AD'   # 서울역
  local a='%EC%84%9C%EC%9A%B8%ED%8A%B9%EB%B3%84%EC%8B%9C%20%EC%A4%91%EA%B5%AC%20%ED%95%9C%EA%B0%95%EB%8C%80%EB%A1%9C%20405'   # 서울특별시 중구 한강대로 405
  if vworld_call "검색" "/location/search?query=${q}&page=1&size=5" \
     && vworld_call "주소 변환" "/location/address?address=${a}"; then
    set_row $I_VW "확인 성공" "$VW_NOTE"
  else
    set_row $I_VW "확인 실패" "$VW_FAIL"
  fi
}

# Lost112: batch /search/total(전) -> POST /search/save -> /search/total(후)
lost_total() { # 문서 수를 LOST_TOTAL에 (실패하면 ?)
  LOST_TOTAL="?"
  http_call GET "${BATCH_URL}/search/total" 30
  show_req GET "/search/total"
  if [ "$HTTP_RC" -eq 0 ] && [ "$HTTP_CODE" = "200" ]; then
    LOST_TOTAL="$(json_num "$HTTP_BODY" result)"
    LOST_TOTAL="${LOST_TOTAL:-?}"
  fi
}

verify_lost112() {
  say "-- Lost112 (수집은 시간이 걸릴 수 있습니다)"
  local before after msg frags frag partial=0 note
  lost_total
  before="$LOST_TOTAL"
  if [ "$HTTP_RC" -ne 0 ]; then
    set_row $I_LOST "확인 실패" "$(conn_fail_msg batch)"
    return
  fi
  http_call POST "${BATCH_URL}/search/save" 600 -d ''
  show_req POST "/search/save"
  if [ "$HTTP_RC" -ne 0 ]; then
    if [ "$HTTP_RC" -eq 28 ]; then
      set_row $I_LOST "확인 실패" "수집 요청 시간 초과(600초): batch 로그 확인 (docker compose logs batch), --skip-lost112-collect로 건너뛰고 LOST112_COLLECT_DAYS를 줄여 보세요"
    else
      set_row $I_LOST "확인 실패" "$(conn_fail_msg batch)"
    fi
    return
  fi
  case "$HTTP_CODE" in
    200)
      frags="$(printf '%s' "$HTTP_BODY" | tr -d '\r\n' | { grep -o '{[[:space:]]*"service"[[:space:]]*:[[:space:]]*"[^"]*"[^}]*}' || true; })"
      if [ -n "$frags" ]; then
        while IFS= read -r frag; do
          say "    서비스 결과: $(clip "$frag" 160)"
        done <<< "$frags"
      else
        say "    요약 본문: $(clip "$HTTP_BODY" 300)"
      fi
      if printf '%s' "$HTTP_BODY" | grep -q '"error"[[:space:]]*:[[:space:]]*"'; then partial=1; fi
      lost_total
      after="$LOST_TOTAL"
      note="문서 수 ${before} -> ${after}"
      if [ "$partial" -eq 1 ]; then
        set_row $I_LOST "부분 성공" "일부 서비스가 error로 끝남(위 서비스 결과 참고, 05 §3·§8); $note"
      elif [ "$after" = "0" ]; then
        set_row $I_LOST "부분 성공" "수집 요청은 성공했으나 문서 수가 0 (기간 내 데이터 없음 또는 응답 구조 가정 확인: 05 §8 Lost112 행); $note"
      else
        set_row $I_LOST "확인 성공" "수집 요청 성공; $note"
      fi ;;
    503)
      set_row $I_LOST "확인 실패" "batch가 LOST112_SERVICE_KEY를 못 읽음(503) -> .env 확인 후 docker compose up -d batch 로 컨테이너를 다시 만드세요" ;;
    502)
      msg="$(json_str "$HTTP_BODY" message)"
      set_row $I_LOST "확인 실패" "수집 전부 실패(502): $(clip "${msg:-$HTTP_BODY}" 200) (키 미등록·승인 대기·트래픽 초과 등은 05 §3·§8 참고)" ;;
    *)
      msg="$(json_str "$HTTP_BODY" message)"
      set_row $I_LOST "확인 실패" "예상 밖 응답 HTTP $HTTP_CODE: $(clip "${msg:-$HTTP_BODY}" 150)" ;;
  esac
}

# FCM: POST /members/login (local) -> POST /alarm/send-fcm/{memberId}. 수신 여부는 이 스크립트가 알 수 없다
verify_fcm() {
  say "-- FCM 테스트 발송 (local 프로필 필요)"
  local token member msg
  http_call POST "${MAIN_URL}/members/login" 30 -H 'Content-Type: application/json' -d "{\"phoneNumber\":\"${FCM_PHONE}\"}"
  show_req POST "/members/login"
  if [ "$HTTP_RC" -ne 0 ]; then
    set_row $I_FCMS "확인 실패" "$(conn_fail_msg main)"
    return
  fi
  if [ "$HTTP_CODE" != "200" ]; then
    msg="$(json_str "$HTTP_BODY" message)"
    case "$HTTP_CODE" in
      401|403|404) set_row $I_FCMS "확인 실패" "로그인 실패(HTTP $HTTP_CODE): $(clip "${msg:-$HTTP_BODY}" 100) -> main이 local 프로필(SPRING_PROFILES_ACTIVE=local)인지, 시드 회원 전화번호(010-0000-0001)인지 확인" ;;
      *) set_row $I_FCMS "확인 실패" "로그인 실패(HTTP $HTTP_CODE): $(clip "${msg:-$HTTP_BODY}" 100)" ;;
    esac
    return
  fi
  token="$(json_str "$HTTP_BODY" accessToken)"
  member="$(json_num "$HTTP_BODY" memberId)"
  if [ -z "$token" ] || [ -z "$member" ]; then
    set_row $I_FCMS "확인 실패" "로그인 응답에서 accessToken·memberId를 찾지 못함"
    return
  fi
  say "    로그인됨: memberId=${member} (토큰은 출력하지 않음)"
  # 본문은 인자(-d) 대신 stdin으로 보낸다: Windows Git Bash에서 curl 인자의 한글이 ANSI로 바뀌어 main이 400을 낸다 (R-91)
  http_call POST "${MAIN_URL}/alarm/send-fcm/${member}" 30 -H 'Content-Type: application/json' -H "access-token: ${token}" \
    --data-binary @- <<< '{"title":"Findear 연동 확인","message":"verify.sh 테스트","type":"test"}'
  show_req POST "/alarm/send-fcm/${member}"
  token=""
  if [ "$HTTP_RC" -ne 0 ]; then
    set_row $I_FCMS "확인 실패" "$(conn_fail_msg main)"
    return
  fi
  if [ "$HTTP_CODE" = "200" ]; then
    set_row $I_FCMS "확인 성공" "발송 요청 성공 - 수신 여부는 이 스크립트가 알 수 없음: 브라우저 알림과 docker compose logs main('FCM 발송 완료')을 확인"
  else
    msg="$(json_str "$HTTP_BODY" message)"
    set_row $I_FCMS "확인 실패" "발송 요청 실패(HTTP $HTTP_CODE): $(clip "${msg:-$HTTP_BODY}" 100)"
  fi
}

if [ $T_VW -eq 1 ]; then verify_vworld; fi
if [ $T_LOST -eq 1 ]; then verify_lost112; fi
if [ $T_FCM -eq 1 ]; then verify_fcm; fi

# --- 3단계: 요약 -----------------------------------------------------------------------------------------------
say ""
say "== 3단계: 요약 =="
say "| 연동 | 상태 | 메모 |"
say "|---|---|---|"
exit_code=0
for i in 0 1 2 3 4; do
  say "| ${NAMES[$i]} | ${STATUS[$i]} | ${MEMO[$i]} |"
  case "${STATUS[$i]}" in
    "설정 오류"|"확인 실패") exit_code=1 ;;
  esac
done
say ""
say "AWS S3: 이 스크립트는 확인하지 않습니다 - 배포 시 infra/aws/README.md"
if [ "${STATUS[$I_FCMP]}" = "설정됨" ]; then
  say "FCM 테스트 페이지: 브라우저에서 확인 (python -m http.server 5500 -d tools/fcm-test -> http://localhost:5500, tools/fcm-test/README.md)"
fi
if [ "$exit_code" -ne 0 ]; then
  say "결과: 설정 오류 또는 확인 실패가 있습니다 (종료 코드 1). 위 메모와 docs/restoration/05-external-integrations.md §9를 보세요."
else
  say "결과: 설정 오류·확인 실패 없음 (종료 코드 0). '미설정'은 키를 아직 채우지 않은 연동입니다."
fi
exit "$exit_code"
