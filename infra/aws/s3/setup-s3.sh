#!/usr/bin/env bash
# AWS S3 버킷 준비 (R-64): 버킷 생성 -> Public Access Block 조정 -> CORS -> images/* 공개 읽기 정책 -> 결과 확인.
# 다시 실행해도 결과가 같다.
#
# 용도
#   배포(U-08)를 결정한 사용자가 한 번 실행한다. 1차 복구 작업에서는 AWS에 연결하지 않았다 (D-41).
#   CORS·버킷 정책의 JSON은 infra/seaweedfs/storage-init.sh 한 곳에서 만든다(로컬 SeaweedFS와 같은 기준). 이 스크립트는 그것을 그대로 실행한다.
#
# 사용 예 (AWS, 관리자 자격증명이 AWS CLI 기본 체인에 있어야 한다: aws configure, AWS_PROFILE, SSO 등)
#   STORAGE_BUCKET=my-findear-images \
#   CORS_ALLOWED_ORIGINS=https://findear.example.com \
#   AWS_REGION=ap-northeast-2 \
#   bash infra/aws/s3/setup-s3.sh            # 계정 확인 프롬프트가 뜬다. 생략하려면 --yes
#
# 입력(환경변수)
#   STORAGE_BUCKET        (필수) 버킷 이름. 전 세계에서 유일해야 한다
#   CORS_ALLOWED_ORIGINS  (필수) 브라우저가 presigned URL로 직접 PUT·GET 할 프론트 주소, 쉼표 구분
#   AWS_REGION            (선택) 기본 ap-northeast-2
#   STORAGE_ENDPOINT      (선택, 로컬 리허설 전용) 값이 있으면 그 엔드포인트(SeaweedFS)로 보내고 AWS 전용 단계(Public Access Block, 계정 확인)를 건너뛴다.
#                         실제 AWS에는 설정하지 않는다
#
# 필요한 권한 (실행하는 사람, 관리자 자격증명. 앱용 키를 만들지 않는다)
#   s3:CreateBucket, s3:ListBucket(head-bucket), s3:PutBucketPublicAccessBlock, s3:GetBucketPublicAccessBlock,
#   s3:PutBucketCORS, s3:GetBucketCORS, s3:PutBucketPolicy, s3:GetBucketPolicy (+ sts:GetCallerIdentity는 권한 불필요)
#
# 로컬 리허설 (AWS 호출 없음) - README.md "로컬 리허설" 참고
#   docker compose up -d seaweedfs 후 aws-cli 컨테이너에서 STORAGE_ENDPOINT=http://seaweedfs:8333 으로 실행
#
# 공식 문서 (2026-10-01 확인, 목록은 infra/aws/README.md)
#   https://docs.aws.amazon.com/cli/latest/reference/s3api/create-bucket.html
#   https://docs.aws.amazon.com/cli/latest/reference/s3api/put-public-access-block.html
#   https://docs.aws.amazon.com/cli/latest/reference/s3api/put-bucket-cors.html
#   https://docs.aws.amazon.com/cli/latest/reference/s3api/put-bucket-policy.html
#   https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-control-block-public-access.html
set -euo pipefail

assume_yes=false
for arg in "$@"; do
  case "$arg" in
    --yes|-y) assume_yes=true ;;
    -h|--help) sed -n '2,/^set -euo/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "알 수 없는 옵션: $arg (--yes, --help만 지원)" >&2; exit 2 ;;
  esac
done

bucket="${STORAGE_BUCKET:?STORAGE_BUCKET이 필요합니다}"
origins="${CORS_ALLOWED_ORIGINS:?CORS_ALLOWED_ORIGINS가 필요합니다}"
region="${AWS_REGION:-ap-northeast-2}"
endpoint="${STORAGE_ENDPOINT:-}"

# 버킷 이름 형식: 소문자·숫자·점·하이픈, 3~63자, 시작과 끝은 소문자·숫자
if ! [[ "$bucket" =~ ^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$ ]]; then
  echo "버킷 이름 형식이 올바르지 않습니다 (소문자·숫자·'.'·'-', 3~63자): $bucket" >&2
  exit 1
fi

# AWS CLI는 리전을 AWS_DEFAULT_REGION 등으로 읽는다. storage-init.sh(자식 프로세스)에도 같은 값이 가도록 내보낸다
export STORAGE_BUCKET="$bucket" CORS_ALLOWED_ORIGINS="$origins" AWS_REGION="$region" AWS_DEFAULT_REGION="$region"
export STORAGE_ENDPOINT="$endpoint"

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
storage_init="$here/../../seaweedfs/storage-init.sh"
if [ ! -f "$storage_init" ]; then
  echo "storage-init.sh를 찾을 수 없습니다: $storage_init" >&2
  exit 1
fi

s3api() {
  if [ -n "$endpoint" ]; then
    aws --endpoint-url "$endpoint" s3api "$@"
  else
    aws s3api "$@"
  fi
}

echo "== S3 준비 요약 =="
echo "  버킷:   $bucket"
echo "  리전:   $region"
echo "  origin: $origins"
if [ -n "$endpoint" ]; then
  echo "  엔드포인트: $endpoint  (로컬 리허설: AWS를 호출하지 않습니다)"
else
  echo "  엔드포인트: AWS 기본 (실제 AWS에 적용됩니다)"
  # 이 단계부터 AWS를 호출한다 (리허설 모드에서는 실행되지 않는다)
  account="$(aws sts get-caller-identity --query Account --output text)"
  echo "  AWS 계정: $account"
  if [ "$assume_yes" != true ]; then
    read -r -p "AWS 계정 $account 에 적용합니다. 계속할까요? [y/N] " answer
    case "$answer" in
      y|Y|yes|YES) ;;
      *) echo "취소했습니다."; exit 1 ;;
    esac
  fi
fi

# 1) 버킷: 없으면 만든다 (us-east-1은 LocationConstraint 없이, 그 외는 LocationConstraint 필요)
if s3api head-bucket --bucket "$bucket" > /dev/null 2>&1; then
  echo "[setup-s3] 버킷 있음: $bucket"
elif [ "$region" = "us-east-1" ]; then
  s3api create-bucket --bucket "$bucket" > /dev/null
  echo "[setup-s3] 버킷 생성: $bucket"
else
  s3api create-bucket --bucket "$bucket" \
    --create-bucket-configuration "LocationConstraint=$region" > /dev/null
  echo "[setup-s3] 버킷 생성: $bucket ($region)"
fi

# 2) AWS 전용: 새 버킷은 Block Public Access 4가지가 모두 켜져 있다.
#    images/* 공개 읽기 "버킷 정책"을 넣으려면 BlockPublicPolicy·RestrictPublicBuckets만 끈다. ACL 기반 공개는 계속 막는다.
#    (Object Ownership은 새 버킷 기본값인 BucketOwnerEnforced = ACL 비활성 그대로 둔다)
if [ -n "$endpoint" ]; then
  echo "[setup-s3] 로컬 리허설: AWS 전용 단계 건너뜀 (put-public-access-block)"
else
  s3api put-public-access-block --bucket "$bucket" \
    --public-access-block-configuration \
    "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=false,RestrictPublicBuckets=false"
  echo "[setup-s3] Public Access Block: ACL 차단 유지, 정책 기반 공개 허용"
fi

# 3) CORS와 images/* 공개 읽기 정책: 로컬과 같은 스크립트를 그대로 쓴다 (버킷이 있으므로 생성은 건너뜀)
sh "$storage_init"

# 4) 결과 확인
echo "== 결과 확인 =="
echo "-- CORS"
s3api get-bucket-cors --bucket "$bucket"
echo "-- 버킷 정책"
s3api get-bucket-policy --bucket "$bucket" --query Policy --output text
if [ -z "$endpoint" ]; then
  echo "-- Public Access Block"
  s3api get-public-access-block --bucket "$bucket"
fi
echo "[setup-s3] 완료: $bucket"
