#!/usr/bin/env bash
# Findear 배포 서버(EC2, Ubuntu 24.04) 초기화 (R-63). 다시 실행해도 결과가 같다.
#
# 하는 일: Docker Engine + compose plugin 설치, 사용자를 docker 그룹에 추가, vm.max_map_count 영구 설정,
#          (swap이 없으면) swap 파일, 앱 디렉토리에 레포 clone, .env(.env.example 복사)·secrets/ 준비.
# 하지 않는 일: 배포(infra/deploy/deploy.sh), git pull, 보안그룹·AWS 리소스 설정.
#
# 사용 (root로 실행. 이 스크립트는 로컬(Windows)에서 실행하지 않는다):
#   레포가 이미 있으면    sudo bash infra/deploy/init-host.sh
#   레포가 아직 없으면    curl -fsSL https://raw.githubusercontent.com/EhighG/Findear/master/infra/deploy/init-host.sh -o init-host.sh
#                         sudo bash init-host.sh              (--user 등 옵션은 --help 참고)
#
# 참고한 공식 문서
#   Docker Engine 설치(Ubuntu, apt 저장소):  https://docs.docker.com/engine/install/ubuntu/
#   설치 후 단계(docker 그룹):               https://docs.docker.com/engine/install/linux-postinstall/
#   vm.max_map_count (Elasticsearch):        https://www.elastic.co/docs/deploy-manage/deploy/self-managed/install-elasticsearch-docker-prod
#                                            (설정 파일은 /etc/sysctl.conf 대신 /etc/sysctl.d/*.conf도 같은 방식으로 읽힌다 — sysctl.d(5))
set -euo pipefail

# Elasticsearch 공식 문서가 현재 요구하는 값. 이 값 이상이면 부트스트랩 검사를 통과한다 (이전 문서의 262144도 만족)
MAX_MAP_COUNT=1048576
SYSCTL_FILE=/etc/sysctl.d/99-findear.conf

TARGET_USER="${SUDO_USER:-}"
APP_DIR=/opt/findear
REPO_URL=https://github.com/EhighG/Findear.git
BRANCH=master
SWAP_SIZE=2G

log() { echo "[init-host] $*"; }
err() { echo "[init-host] 오류: $*" >&2; }

usage() {
  cat <<'EOF'
사용: sudo bash init-host.sh [옵션]

  --user <이름>        docker 그룹에 넣고 앱 디렉토리·.env의 소유자로 쓸 사용자 (기본: $SUDO_USER)
  --app-dir <경로>     앱 디렉토리 (기본: /opt/findear)
  --repo <URL>         clone할 레포 (기본: https://github.com/EhighG/Findear.git)
  --branch <이름>      clone할 브랜치 (기본: master)
  --swap-size <크기>   swap 파일 크기, 예: 2G (기본: 2G, 0이면 만들지 않음). 활성 swap이 이미 있으면 만들지 않는다
  -h, --help           이 도움말

Ubuntu 24.04, root 권한(sudo)이 필요하다. 다시 실행해도 이미 된 단계는 건너뛴다.
EOF
}

while [ $# -gt 0 ]; do
  case "$1" in
    --user)      TARGET_USER="${2:?--user 값이 필요합니다}"; shift 2 ;;
    --app-dir)   APP_DIR="${2:?--app-dir 값이 필요합니다}"; shift 2 ;;
    --repo)      REPO_URL="${2:?--repo 값이 필요합니다}"; shift 2 ;;
    --branch)    BRANCH="${2:?--branch 값이 필요합니다}"; shift 2 ;;
    --swap-size) SWAP_SIZE="${2:?--swap-size 값이 필요합니다}"; shift 2 ;;
    -h|--help)   usage; exit 0 ;;
    *)           err "알 수 없는 옵션: $1"; usage >&2; exit 2 ;;
  esac
done

# --- 1. 환경 확인 ---
if [ ! -r /etc/os-release ]; then
  err "/etc/os-release가 없습니다. Ubuntu에서 실행하세요."
  exit 1
fi
# shellcheck disable=SC1091  # 대상 서버의 파일이라 로컬 shellcheck가 따라갈 수 없다
. /etc/os-release
if [ "${ID:-}" != "ubuntu" ]; then
  err "Ubuntu에서만 실행합니다 (현재: ${ID:-알 수 없음})."
  exit 1
fi
if [ "$(id -u)" -ne 0 ]; then
  err "root로 실행하세요: sudo bash $0"
  exit 1
fi
if [ -z "$TARGET_USER" ] || [ "$TARGET_USER" = "root" ]; then
  err "앱을 운영할 일반 사용자를 --user <이름>으로 지정하세요 (sudo로 실행하면 기본값은 \$SUDO_USER)."
  exit 1
fi
if ! id "$TARGET_USER" > /dev/null 2>&1; then
  err "사용자가 없습니다: $TARGET_USER"
  exit 1
fi
log "Ubuntu ${VERSION_ID:-?} / 사용자 $TARGET_USER / 앱 디렉토리 $APP_DIR"

# --- 2. Docker Engine + compose plugin ---
if docker compose version > /dev/null 2>&1; then
  log "Docker 설치됨, 건너뜀: $(docker --version)"
else
  log "Docker Engine 설치 (apt 저장소 방식, 공식 문서 기준)"
  export DEBIAN_FRONTEND=noninteractive
  # 충돌 패키지 제거 (없으면 아무 일도 하지 않는다)
  conflicting=$(dpkg --get-selections docker.io docker-compose docker-compose-v2 docker-doc docker-buildx podman-docker containerd runc 2> /dev/null | cut -f1 || true)
  if [ -n "$conflicting" ]; then
    # shellcheck disable=SC2086  # 패키지 이름 목록이라 단어 분리가 필요하다
    apt-get remove -y $conflicting
  fi
  apt-get update
  apt-get install -y ca-certificates curl
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
  cat > /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: ${UBUNTU_CODENAME:-$VERSION_CODENAME}
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF
  apt-get update
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  systemctl enable --now docker
fi
# compose.prod.yml의 !override 태그는 Compose 문서에 도입 버전이 적혀 있지 않아 최소 버전을 검사하지 않는다.
# deploy.sh --check가 docker compose config로 실제로 읽히는지 확인한다.
docker compose version

# --- 3. docker 그룹 ---
if id -nG "$TARGET_USER" | tr ' ' '\n' | grep -qx docker; then
  log "$TARGET_USER 는 이미 docker 그룹, 건너뜀"
else
  getent group docker > /dev/null || groupadd docker
  usermod -aG docker "$TARGET_USER"
  log "$TARGET_USER 를 docker 그룹에 추가했습니다. 다시 로그인해야 적용됩니다 (ssh 재접속)."
fi

# --- 4. vm.max_map_count (Elasticsearch 필수) ---
current_map_count=$(sysctl -n vm.max_map_count)
target_map_count=$MAX_MAP_COUNT
if [ "$current_map_count" -gt "$MAX_MAP_COUNT" ]; then
  # 이미 더 큰 값이면 낮추지 않는다. 재부팅 뒤에도 낮아지지 않게 파일에도 그 값을 쓴다
  target_map_count=$current_map_count
  log "vm.max_map_count=$current_map_count (> $MAX_MAP_COUNT), 낮추지 않고 이 값을 유지합니다"
elif [ "$current_map_count" -eq "$MAX_MAP_COUNT" ]; then
  log "vm.max_map_count=$current_map_count, 이미 맞음"
else
  log "vm.max_map_count $current_map_count -> $MAX_MAP_COUNT"
  sysctl -w "vm.max_map_count=$MAX_MAP_COUNT" > /dev/null
fi
# 재부팅 후에도 유지되게 파일로 남긴다 (내용이 같으면 다시 쓰지 않는다)
desired_sysctl="vm.max_map_count=$target_map_count"
if [ "$(cat "$SYSCTL_FILE" 2> /dev/null || true)" = "$desired_sysctl" ]; then
  log "$SYSCTL_FILE 확인됨"
else
  echo "$desired_sysctl" > "$SYSCTL_FILE"
  log "$SYSCTL_FILE 작성"
fi

# --- 5. swap ---
# 메모리: compose.yml 기본 제한(최소 사양) 합계가 약 3.9GB (docs/restoration/04-target-architecture.md §5).
# 4GB급 인스턴스는 OS 몫이 모자라 swap 2GB 이상이 있어야 겨우 기동한다. 8GB급이면 필요 없다 (--swap-size 0).
if [ "$SWAP_SIZE" = "0" ]; then
  log "--swap-size 0: swap을 만들지 않습니다"
elif [ -n "$(swapon --show --noheadings)" ]; then
  log "활성 swap이 있어 건너뜀: $(swapon --show --noheadings | tr -s ' ' | tr '\n' ';')"
else
  log "swap 파일 생성: /swapfile ($SWAP_SIZE)"
  if [ -e /swapfile ]; then
    # 이전 실행이 중간에 끊긴 흔적일 수 있다. 활성이 아니므로 새로 만든다
    rm -f /swapfile
  fi
  fallocate -l "$SWAP_SIZE" /swapfile
  chmod 600 /swapfile
  mkswap /swapfile > /dev/null
  swapon /swapfile
  if ! grep -qE '^/swapfile[[:space:]]' /etc/fstab; then
    echo '/swapfile none swap sw 0 0' >> /etc/fstab
  fi
  log "swap 활성: $(swapon --show --noheadings | tr -s ' ')"
fi

# --- 6. 앱 디렉토리 (레포 clone) ---
if [ -d "$APP_DIR/.git" ] || [ -e "$APP_DIR/compose.yml" ]; then
  # 이미 clone돼 있으면 pull하지 않는다. 업데이트는 deploy.sh가 한다
  log "앱 디렉토리 있음, 건너뜀: $APP_DIR"
else
  if ! command -v git > /dev/null 2>&1; then
    log "git 설치"
    DEBIAN_FRONTEND=noninteractive apt-get update
    DEBIAN_FRONTEND=noninteractive apt-get install -y git
  fi
  if [ -e "$APP_DIR" ] && [ -n "$(ls -A "$APP_DIR" 2> /dev/null)" ]; then
    err "$APP_DIR 가 비어 있지 않고 레포도 아닙니다. 다른 --app-dir을 쓰거나 비우세요."
    exit 1
  fi
  log "clone: $REPO_URL ($BRANCH) -> $APP_DIR"
  mkdir -p "$(dirname "$APP_DIR")"
  git clone --branch "$BRANCH" "$REPO_URL" "$APP_DIR"
  chown -R "$TARGET_USER": "$APP_DIR"
fi

# --- 7. .env, secrets/ ---
if [ -e "$APP_DIR/.env" ]; then
  log ".env 있음, 건너뜀"
else
  if [ ! -f "$APP_DIR/.env.example" ]; then
    err "$APP_DIR/.env.example 이 없습니다."
    exit 1
  fi
  cp "$APP_DIR/.env.example" "$APP_DIR/.env"
  chown "$TARGET_USER": "$APP_DIR/.env"
  chmod 600 "$APP_DIR/.env"
  log ".env 생성 (.env.example 복사, 600). 값을 채워야 합니다."
fi
# secrets/: compose.yml이 디렉토리째 /run/secrets 로 마운트하고(ro), main 컨테이너는 uid/gid 10001 로 실행된다
# (main/Dockerfile: groupadd --system --gid 10001 findear / useradd --uid 10001). 디렉토리가 700 이면 파일 권한과 무관하게
# 컨테이너 사용자가 디렉토리에 들어가지 못해 FCM 파일을 읽을 수 없다 → 소유자 $TARGET_USER, 그룹 gid 10001, 모드 2750.
# setgid(2)를 두어 이 안에서 새로 만든 파일이 그룹 10001을 받게 한다: 배포 사용자는 그룹 10001에 속하지 않아
# sudo 없이는 chgrp 10001 을 할 수 없기 때문이다 (다른 곳에서 mv로 옮겨 온 파일은 그룹이 그대로라 sudo chgrp 필요).
# 그룹을 숫자(10001)로 지정하므로 호스트에 그 gid의 그룹이 없어도 된다. 이미 있어도 매번 맞춘다 (재실행 안전).
mkdir -p "$APP_DIR/secrets"
chown "$TARGET_USER:10001" "$APP_DIR/secrets"
chmod 2750 "$APP_DIR/secrets"
log "secrets/ 소유자 $TARGET_USER, 그룹 10001, 모드 2750 (컨테이너 사용자 uid/gid 10001이 그룹으로 읽음, 새 파일은 그룹 10001)"

# --- 8. 다음 할 일 ---
cat <<EOF

[init-host] 완료. 다음 할 일:

  1. 다시 로그인 (docker 그룹 적용): exit 후 ssh 재접속

  2. $APP_DIR/.env 를 편집 ($TARGET_USER 로 — .env 편집에는 sudo가 필요 없다). deploy.sh --check 가 아래를 검사한다:
       - 새 값으로 바꿀 것 (.env.example 예시 값이면 거부):
           JWT_SECRET (openssl rand -base64 32), MYSQL_PASSWORD, MYSQL_ROOT_PASSWORD,
           MYSQL_EXPORTER_PASSWORD, GRAFANA_ADMIN_PASSWORD
       - 새로 채울 것 (비어 있으면 거부): REDIS_PASSWORD, ELASTIC_PASSWORD
       - 비밀번호 값에는 \$ ' \\ 를 쓰지 않는다 (Compose 변수 치환·exporter 계정 SQL 때문)
       - AWS S3: STORAGE_BUCKET, STORAGE_PUBLIC_BASE_URL=https://{버킷}.s3.ap-northeast-2.amazonaws.com (localhost면 거부),
                 STORAGE_PATH_STYLE=false, AWS_ACCESS_KEY_ID·AWS_SECRET_ACCESS_KEY 는 비움 (EC2 IAM Role)
       - NAVER_REDIRECT_URI, CORS_ALLOWED_ORIGINS 는 배포 주소로
       - FCM_ENABLED=true 이면 서비스계정 파일을 $APP_DIR/secrets/firebase-adminsdk.json 에 두고
           sudo chgrp 10001 $APP_DIR/secrets/firebase-adminsdk.json
           chmod 640 $APP_DIR/secrets/firebase-adminsdk.json
         (컨테이너 사용자 uid/gid 10001 이 그룹으로 읽는다. secrets/ 는 소유자 $TARGET_USER, 그룹 10001, 2750(setgid) —
          secrets/ 안에서 새로 만든 파일은 이미 그룹 10001이라 chgrp가 필요 없지만, 다른 곳에서 옮겨 온 파일은 필요하다.
          배포 사용자는 그룹 10001에 속하지 않으므로 chgrp에는 sudo가 필요하다)

  3. GHCR 패키지(findear-main, -batch, -match)가 public 인지 확인한다 (docs/restoration/09-deploy-and-aws.md §1).
     private 이면 인증 없이 pull 할 수 없다.

  4. 보안그룹: 22 는 관리자 IP 만, 80 만 연다. 9100(node-exporter)·3000(Grafana)·9090(Prometheus) 등은 열지 않는다.
     Docker 게시 포트는 UFW 를 우회하므로 방화벽은 보안그룹으로 관리한다. Grafana 는 SSH 터널로 접근한다.

  5. AWS S3 버킷·IAM Role 준비: $APP_DIR/infra/aws/README.md (R-64에서 추가)

  6. 설정 검사 후 배포 (compose.prod.yml 의 !override 를 현재 compose plugin 이 읽는지도 여기서 확인된다):
       cd $APP_DIR
       infra/deploy/deploy.sh --check
       infra/deploy/deploy.sh
EOF
