#!/bin/sh
# SeaweedFS S3 자격증명 파일을 환경변수로 만든 뒤 이미지 기본 entrypoint로 넘긴다 (R-13).
# 키를 레포에 두지 않기 위해 s3.json.template의 자리표시자를 컨테이너 안에서만 채운다.
# 공개 읽기는 identity가 아니라 버킷 정책으로 준다 (storage-init.sh, D-45).
set -eu

: "${AWS_ACCESS_KEY_ID:?SeaweedFS용 AWS_ACCESS_KEY_ID를 .env에 설정하세요}"
: "${AWS_SECRET_ACCESS_KEY:?SeaweedFS용 AWS_SECRET_ACCESS_KEY를 .env에 설정하세요}"

config=/tmp/s3.json
sed -e "s|__AWS_ACCESS_KEY_ID__|${AWS_ACCESS_KEY_ID}|" \
    -e "s|__AWS_SECRET_ACCESS_KEY__|${AWS_SECRET_ACCESS_KEY}|" \
    /etc/seaweedfs/s3.json.template > "$config"
# 이미지 entrypoint가 seaweed 사용자로 권한을 낮춰 실행하므로 그 사용자만 읽을 수 있게 한다
chown seaweed:seaweed "$config"
chmod 600 "$config"

exec /entrypoint.sh "$@"
