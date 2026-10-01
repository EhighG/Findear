#!/usr/bin/env bash
# Findear 배포 (R-63). 배포 서버에서 일반 사용자(docker 그룹)로 실행한다. 서버에서 이미지를 빌드하지 않는다 —
# GHCR에서 pull한다 (이미지는 Actions의 images.yml이 올린다, docs/restoration/09-deploy-and-aws.md §1).
#
#   infra/deploy/deploy.sh --check              # 설정 검사만 (.env 검사 + compose config). git·pull·up 없음
#   infra/deploy/deploy.sh                      # 업데이트/첫 배포: git pull → 검사 → pull → up → ps
#   infra/deploy/deploy.sh --tag <커밋 SHA>     # 롤백 또는 특정 버전 배포 (이번 실행에만 IMAGE_TAG를 덮어씀)
#   infra/deploy/deploy.sh --no-git             # git pull 생략 (이미 맞는 커밋을 체크아웃한 경우)
#
# 롤백 시 주의: --tag는 이미지만 이전 SHA로 돌린다. 레포의 compose 파일·설정은 현재 것 그대로다.
#   구성까지 되돌리려면:  git checkout <SHA> && infra/deploy/deploy.sh --no-git --tag <SHA>
#   (되돌린 뒤 최신으로 가려면 git checkout master). --tag 없이 .env의 IMAGE_TAG를 바꿔도 된다.
#
# 사용하는 compose 파일은 compose.yml + compose.prod.yml 뿐이다 (-f를 명시해 로컬 전용 compose.override.yml이 섞이지 않게 한다).
# .env는 source하지 않고 필요한 KEY=값 줄만 읽는다 (값에 공백·따옴표가 있다).
#
# 참고한 공식 문서
#   Compose 변수 우선순위(셸 환경변수가 .env보다 우선): https://docs.docker.com/compose/how-tos/environment-variables/envvars-precedence/
#   docker compose up (--wait, --wait-timeout):          https://docs.docker.com/reference/cli/docker/compose/up/
set -euo pipefail

WAIT_TIMEOUT="${WAIT_TIMEOUT:-600}"   # up --wait 최대 대기(초). 첫 기동은 ES·MySQL 때문에 오래 걸린다

log() { echo "[deploy] $*"; }

usage() {
  cat <<'EOF'
사용: infra/deploy/deploy.sh [옵션]

  --check          설정 검사만 하고 끝낸다 (.env 검사, compose config, 사용할 이미지 출력). git·pull·up 없음
  --tag <TAG>      이번 실행에 쓸 IMAGE_TAG (커밋 SHA 등). .env의 IMAGE_TAG보다 우선한다
  --no-git         git pull을 하지 않는다
  -h, --help       이 도움말

환경변수: WAIT_TIMEOUT (up --wait 대기 초, 기본 600)
EOF
}

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

main() {
  local do_check=0 do_git=1 tag=""
  local orig_args=("$@")   # git pull 뒤 재실행에 쓸 원래 인자 (공백이 든 인자도 그대로 보존된다)
  local script_path
  script_path="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/$(basename "${BASH_SOURCE[0]}")"
  while [ $# -gt 0 ]; do
    case "$1" in
      --check)   do_check=1; shift ;;
      --tag)     tag="${2:?--tag 값이 필요합니다}"; shift 2 ;;
      --no-git)  do_git=0; shift ;;
      -h|--help) usage; return 0 ;;
      *)         echo "[deploy] 알 수 없는 옵션: $1" >&2; usage >&2; return 2 ;;
    esac
  done

  local root
  root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
  cd "$root"
  local compose=(docker compose -f compose.yml -f compose.prod.yml)

  if [ -n "$tag" ]; then
    if ! [[ "$tag" =~ ^[A-Za-z0-9_][A-Za-z0-9._-]{0,127}$ ]]; then
      echo "[deploy] 오류: 이미지 태그 형식이 올바르지 않습니다: $tag" >&2
      return 1
    fi
    # 셸 환경변수는 .env보다 우선한다 (Compose 변수 우선순위)
    export IMAGE_TAG="$tag"
    log "이번 실행의 IMAGE_TAG=$IMAGE_TAG (.env보다 우선)"
  fi

  # --- 0. git pull ---
  if [ "$do_check" -eq 0 ] && [ "$do_git" -eq 1 ]; then
    if [ -n "$(git status --porcelain)" ]; then
      echo "[deploy] 오류: 작업 트리에 변경이 있어 git pull을 하지 않습니다. 정리한 뒤 다시 실행하세요 (git status 참고)." >&2
      return 1
    fi
    local head_before head_after
    head_before="$(git rev-parse HEAD)"
    log "git pull --ff-only"
    git pull --ff-only
    head_after="$(git rev-parse HEAD)"
    if [ "$head_before" != "$head_after" ]; then
      # pull로 deploy.sh 자신이나 compose 파일이 바뀌었을 수 있다. 지금 실행 중인 것은 옛 로직이므로
      # 새 deploy.sh를 처음부터 다시 실행한다 (검사·up이 새 로직으로 돌게).
      # --no-git 을 앞에 붙여 다시 pull하지 않으므로 무한 반복이 없다. 원래 인자(--tag 등)는 main 시작 때
      # orig_args 배열에 보관해 그대로 넘긴다 (--no-git이 이미 들어 있어도 해롭지 않다).
      log "git pull로 ${head_before:0:7} -> ${head_after:0:7} 갱신됨: 새 deploy.sh로 다시 실행합니다"
      # bash로 실행해 파일 실행 권한에 기대지 않는다 (bash infra/deploy/deploy.sh 로 실행한 경우에도 동작)
      exec bash "$script_path" --no-git ${orig_args[@]+"${orig_args[@]}"}
    fi
  fi

  # --- 1. .env 검사 ---
  local env_file="$root/.env" example_file="$root/.env.example"
  local errors=() warnings=()
  if [ ! -f "$env_file" ]; then
    errors+=(".env 없음: $env_file (.env.example을 복사해 값을 채우세요)")
  elif [ ! -f "$example_file" ]; then
    errors+=(".env.example 없음: $example_file (예시 값과 비교할 수 없습니다)")
  else
    local key value example
    # 예시 값 그대로 쓰는 비밀값 (예시는 .env.example에서 읽는다)
    for key in JWT_SECRET MYSQL_PASSWORD MYSQL_ROOT_PASSWORD MYSQL_EXPORTER_PASSWORD GRAFANA_ADMIN_PASSWORD AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY; do
      value="$(env_get "$env_file" "$key")"
      example="$(env_get "$example_file" "$key")"
      if [ -n "$example" ] && [ "$value" = "$example" ]; then
        errors+=("$key 가 .env.example의 예시 값 그대로입니다 (새 값으로 바꾸세요; AWS_* 는 EC2 IAM Role을 쓰면 비웁니다)")
      fi
    done
    # 비어 있으면 안 되는 값
    for key in REDIS_PASSWORD ELASTIC_PASSWORD JWT_SECRET MYSQL_PASSWORD MYSQL_ROOT_PASSWORD MYSQL_EXPORTER_PASSWORD GRAFANA_ADMIN_PASSWORD STORAGE_BUCKET STORAGE_PUBLIC_BASE_URL; do
      value="$(env_get "$env_file" "$key")"
      if [ -z "$value" ]; then
        errors+=("$key 가 비어 있습니다")
      fi
    done
    # 배포 스토리지는 AWS S3 (09 §4)
    value="$(env_get "$env_file" STORAGE_PUBLIC_BASE_URL)"
    case "${value,,}" in
      *localhost*|*127.0.0.1*) errors+=("STORAGE_PUBLIC_BASE_URL 에 localhost/127.0.0.1 이 들어 있습니다 (배포는 AWS S3: https://{버킷}.s3.ap-northeast-2.amazonaws.com)") ;;
    esac
    # compose.prod.yml 머리 주석의 제약: $ 는 Compose 변수 치환, ' 와 \ 는 exporter 계정 SQL·셸에서 깨진다
    for key in MYSQL_PASSWORD MYSQL_ROOT_PASSWORD MYSQL_EXPORTER_PASSWORD GRAFANA_ADMIN_PASSWORD REDIS_PASSWORD ELASTIC_PASSWORD; do
      value="$(env_get "$env_file" "$key")"
      case "$value" in
        *\$*|*\'*|*\\*) errors+=("$key 에 \$, 작은따옴표('), 역슬래시(\\) 중 하나가 들어 있습니다 (쓰지 마세요)") ;;
      esac
    done

    # 경고 (계속 진행)
    value="$(env_get "$env_file" STORAGE_PATH_STYLE)"
    if [ "${value,,}" = "true" ]; then
      warnings+=("STORAGE_PATH_STYLE=true 입니다 (SeaweedFS용 값. AWS S3는 false 권장)")
    fi
    value="$(env_get "$env_file" AWS_ACCESS_KEY_ID)"
    if [ -n "$value" ] && [ "$value" != "$(env_get "$example_file" AWS_ACCESS_KEY_ID)" ]; then
      warnings+=("AWS_ACCESS_KEY_ID 가 비어 있지 않습니다 (EC2 IAM Role을 쓰면 비웁니다, 09 §4)")
    fi
    value="$(env_get "$env_file" SPRING_PROFILES_ACTIVE)"
    if [ -n "$value" ] && [ "$value" != "prod" ]; then
      warnings+=("SPRING_PROFILES_ACTIVE=$value (compose.prod.yml이 prod로 고정하므로 영향은 없습니다)")
    fi
    value="$(env_get "$env_file" JWT_SECRET)"
    if [ -n "$value" ]; then
      local jwt_bytes
      if jwt_bytes="$(printf '%s' "$value" | base64 -d 2> /dev/null | wc -c)" && [ -n "$jwt_bytes" ]; then
        if [ "$jwt_bytes" -lt 32 ]; then
          warnings+=("JWT_SECRET 의 Base64 디코딩 길이가 ${jwt_bytes}바이트입니다 (256bit = 32바이트 이상 권장: openssl rand -base64 32)")
        fi
      else
        warnings+=("JWT_SECRET 의 Base64 길이를 확인하지 못했습니다 (base64 -d 실패)")
      fi
    fi
    # FCM: 켜져 있는데 서비스계정 파일이 없으면 main 기동이 실패한다 → 오류
    value="$(env_get "$env_file" FCM_ENABLED)"
    if [ "${value,,}" = "true" ]; then
      local fcm_path fcm_file
      fcm_path="$(env_get "$env_file" FCM_CREDENTIALS_PATH)"
      fcm_path="${fcm_path:-/run/secrets/firebase-adminsdk.json}"
      case "$fcm_path" in
        /run/secrets/*)
          fcm_file="$root/secrets/${fcm_path#/run/secrets/}"
          if [ ! -f "$fcm_file" ]; then
            errors+=("FCM_ENABLED=true 인데 $fcm_file 이 없습니다 (main 기동이 실패합니다. 컨테이너 uid/gid 10001이 읽게 sudo chgrp 10001 + chmod 640)")
          fi ;;
        *)
          warnings+=("FCM_CREDENTIALS_PATH=$fcm_path 는 ./secrets 마운트(/run/secrets) 밖이라 파일 존재를 확인하지 못했습니다") ;;
      esac
    fi
  fi

  local item
  if [ "${#warnings[@]}" -gt 0 ]; then
    for item in "${warnings[@]}"; do echo "[deploy] 경고: $item" >&2; done
  fi
  if [ "${#errors[@]}" -gt 0 ]; then
    for item in "${errors[@]}"; do echo "[deploy] 오류: $item" >&2; done
    echo "[deploy] .env 검사 실패 (${#errors[@]}건). 고친 뒤 다시 실행하세요." >&2
    return 1
  fi
  log ".env 검사 통과"

  # --- 2. compose config ---
  if ! command -v docker > /dev/null 2>&1; then
    echo "[deploy] 오류: docker 명령이 없습니다 (init-host.sh로 설치하세요)." >&2
    return 1
  fi
  if ! "${compose[@]}" config --quiet; then
    echo "[deploy] 오류: docker compose config 실패 (위 메시지 참고)" >&2
    return 1
  fi
  log "compose config 통과"

  # --- 3. 사용할 이미지 ---
  log "사용할 이미지 (IMAGE_TAG=${IMAGE_TAG:-.env 또는 기본값}):"
  "${compose[@]}" config --images | sort -u | sed 's/^/  /'

  # --- 4. --check 는 여기까지 ---
  if [ "$do_check" -eq 1 ]; then
    log "검사 통과 (--check: pull·up은 하지 않았습니다)"
    return 0
  fi

  # --- 5. pull ---
  log "이미지 pull"
  "${compose[@]}" pull

  # --- 6. up ---
  # --wait: 헬스체크가 있는 서비스는 healthy, 없는 서비스(redis-exporter 등)는 running이면 통과한다.
  # 전체 스택에서는 main·batch가 flyway에 service_completed_successfully로 의존하므로, up --wait는 flyway가
  # 성공(exit 0)으로 끝날 때까지 기다리고 flyway가 실패하면 "didn't complete successfully"로 up이 실패한다 (R-63 검증 실험).
  # 서비스 일부만 지정해 실행하면 이 의존이 빠져 경쟁이 생긴다 (flyway가 아직 실행 중이면 통과, 기다리는 도중 끝나면
  # "exited (0)" 오류). 그래서 deploy.sh는 서비스를 지정하지 않고 전체를 올린다.
  log "up -d (--wait, 최대 ${WAIT_TIMEOUT}초)"
  if ! "${compose[@]}" up -d --remove-orphans --wait --wait-timeout "$WAIT_TIMEOUT"; then
    echo "[deploy] 오류: 서비스가 ${WAIT_TIMEOUT}초 안에 정상 상태가 되지 않았습니다." >&2
    "${compose[@]}" ps >&2 || true
    echo "[deploy] 로그 확인: ${compose[*]} logs --tail=100 <서비스>" >&2
    return 1
  fi

  # --- 7. 결과 ---
  "${compose[@]}" ps
  log "사용한 이미지:"
  "${compose[@]}" config --images | sort -u | sed 's/^/  /'
  log "롤백: infra/deploy/deploy.sh --tag <이전 커밋 SHA>  (또는 .env의 IMAGE_TAG를 바꾸고 다시 실행)"
}

# 함수로 감싸 파일 전체를 읽은 뒤 실행한다: git pull이 이 스크립트를 바꿔도 실행 중인 스크립트가 어긋나지 않는다
main "$@"
exit $?
