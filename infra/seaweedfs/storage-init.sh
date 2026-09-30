#!/bin/sh
# 이미지 버킷 준비 (R-13): 버킷 생성 → CORS → 공개 읽기 정책. 다시 실행해도 결과가 같다.
# AWS에서 쓸 aws-cli 명령과 같은 명령을 쓴다. STORAGE_ENDPOINT가 비어 있으면 AWS 기본 엔드포인트로 간다.
# 설계: docs/restoration/06-db-and-config.md §4 (공개 읽기는 images/*만, CORS는 PUT·GET·HEAD)
set -eu

bucket="${STORAGE_BUCKET:?STORAGE_BUCKET이 필요합니다}"
origins="${CORS_ALLOWED_ORIGINS:?CORS_ALLOWED_ORIGINS가 필요합니다}"
region="${AWS_REGION:-ap-northeast-2}"
endpoint="${STORAGE_ENDPOINT:-}"

s3api() {
  if [ -n "$endpoint" ]; then
    aws --endpoint-url "$endpoint" s3api "$@"
  else
    aws s3api "$@"
  fi
}

# 1) 버킷: 이미 있으면 그대로 둔다. us-east-1은 LocationConstraint를 넣지 않는 것이 AWS 규칙
if s3api head-bucket --bucket "$bucket" > /dev/null 2>&1; then
  echo "[storage-init] 버킷 있음: $bucket"
elif [ "$region" = "us-east-1" ]; then
  s3api create-bucket --bucket "$bucket" > /dev/null
  echo "[storage-init] 버킷 생성: $bucket"
else
  s3api create-bucket --bucket "$bucket" \
    --create-bucket-configuration "LocationConstraint=$region" > /dev/null
  echo "[storage-init] 버킷 생성: $bucket ($region)"
fi

# 2) CORS: 브라우저가 presigned URL로 직접 PUT·GET 한다. origin은 CORS_ALLOWED_ORIGINS(쉼표 구분)
origins_json=$(printf '%s' "$origins" | awk -F',' '{
  for (i = 1; i <= NF; i++) {
    gsub(/^[ \t]+|[ \t]+$/, "", $i)
    if ($i != "") { printf "%s\"%s\"", (n++ ? "," : ""), $i }
  }
}')
cat > /tmp/cors.json <<EOF
{
  "CORSRules": [
    {
      "AllowedOrigins": [${origins_json}],
      "AllowedMethods": ["PUT", "GET", "HEAD"],
      "AllowedHeaders": ["*"],
      "ExposeHeaders": ["ETag"],
      "MaxAgeSeconds": 3000
    }
  ]
}
EOF
s3api put-bucket-cors --bucket "$bucket" --cors-configuration file:///tmp/cors.json
echo "[storage-init] CORS 적용: ${origins}"

# 3) 공개 읽기: images/* 의 GetObject만 누구나 가능. 나머지 경로와 쓰기는 자격증명 필요
cat > /tmp/policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadImages",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::${bucket}/images/*"
    }
  ]
}
EOF
s3api put-bucket-policy --bucket "$bucket" --policy file:///tmp/policy.json
echo "[storage-init] 공개 읽기 정책 적용: ${bucket}/images/*"
