#!/usr/bin/env bash
# EC2용 IAM Role + Instance Profile 준비 (R-64): 서버(EC2)의 main이 S3를 쓰는 데 필요한 최소 권한만 준다.
# 다시 실행해도 결과가 같다 (있으면 건너뛰고, 정책은 같은 이름으로 덮어쓴다).
#
# 용도
#   배포(U-08)를 결정한 사용자가 한 번 실행한다. 1차 복구 작업에서는 AWS에 연결하지 않았다 (D-41).
#   만든 Instance Profile을 EC2에 붙이면 main이 SDK 기본 자격증명 체인으로 키 없이 S3를 쓴다 (서버 .env에 AWS 키를 넣지 않는다).
#
# 사용 예
#   STORAGE_BUCKET=my-findear-images bash infra/aws/iam/setup-iam.sh            # 계정 확인 프롬프트가 뜬다. 생략하려면 --yes
#   STORAGE_BUCKET=my-findear-images bash infra/aws/iam/setup-iam.sh --render-only   # AWS를 부르지 않고 치환된 정책 JSON만 출력 (검토용)
#
# 입력(환경변수)
#   STORAGE_BUCKET         (필수) 버킷 이름 (소문자·숫자·'.'·'-', 3~63자)
#   ROLE_NAME              (선택) 기본 findear-app
#   INSTANCE_PROFILE_NAME  (선택) 기본 ROLE_NAME과 같음
#   POLICY_NAME            (선택) 기본 findear-app-s3 (Role의 인라인 정책 이름)
#
# 필요한 권한 (실행하는 사람, IAM 관리자 자격증명)
#   iam:GetRole, iam:CreateRole, iam:PutRolePolicy, iam:GetInstanceProfile, iam:CreateInstanceProfile, iam:AddRoleToInstanceProfile
#   (EC2에 붙일 때는 ec2:AssociateIamInstanceProfile, iam:PassRole)
#
# 공식 문서 (2026-10-01 확인, 목록은 infra/aws/README.md)
#   https://docs.aws.amazon.com/cli/latest/reference/iam/create-role.html
#   https://docs.aws.amazon.com/cli/latest/reference/iam/put-role-policy.html
#   https://docs.aws.amazon.com/cli/latest/reference/iam/create-instance-profile.html
#   https://docs.aws.amazon.com/cli/latest/reference/iam/add-role-to-instance-profile.html
#   https://docs.aws.amazon.com/cli/latest/reference/ec2/associate-iam-instance-profile.html
#   https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_use_switch-role-ec2.html
set -euo pipefail

assume_yes=false
render_only=false
for arg in "$@"; do
  case "$arg" in
    --yes|-y) assume_yes=true ;;
    --render-only) render_only=true ;;
    -h|--help) sed -n '2,/^set -euo/p' "$0" | sed '$d' | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "알 수 없는 옵션: $arg (--yes, --render-only, --help만 지원)" >&2; exit 2 ;;
  esac
done

bucket="${STORAGE_BUCKET:?STORAGE_BUCKET이 필요합니다}"
role_name="${ROLE_NAME:-findear-app}"
profile_name="${INSTANCE_PROFILE_NAME:-$role_name}"
policy_name="${POLICY_NAME:-findear-app-s3}"

# 버킷 이름 형식: 소문자·숫자·점·하이픈, 3~63자, 시작과 끝은 소문자·숫자 (치환 전에 검사 - sed 치환에 특수문자가 들어가지 않게)
if ! [[ "$bucket" =~ ^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$ ]]; then
  echo "버킷 이름 형식이 올바르지 않습니다 (소문자·숫자·'.'·'-', 3~63자): $bucket" >&2
  exit 1
fi
# IAM 이름 형식: 영문·숫자와 _+=,.@- , 역할 이름은 64자 이하
for name in "$role_name" "$profile_name" "$policy_name"; do
  if ! [[ "$name" =~ ^[A-Za-z0-9_+=,.@-]{1,64}$ ]]; then
    echo "IAM 이름 형식이 올바르지 않습니다 (영문·숫자·_+=,.@-, 64자 이하): $name" >&2
    exit 1
  fi
done

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
template="$here/findear-app-policy.json"
trust_policy="$here/ec2-trust-policy.json"

tmp_policy="$(mktemp)"
trap 'rm -f "$tmp_policy"' EXIT
sed "s/__BUCKET__/$bucket/g" "$template" > "$tmp_policy"
if grep -q '__BUCKET__' "$tmp_policy"; then
  echo "정책 템플릿에 치환되지 않은 __BUCKET__이 남았습니다" >&2
  exit 1
fi

# --render-only는 여기서 끝난다. 아래의 aws 명령은 한 번도 실행되지 않는다
if [ "$render_only" = true ]; then
  cat "$tmp_policy"
  exit 0
fi

# 여기부터 AWS를 호출한다
echo "== IAM 준비 요약 =="
echo "  버킷: $bucket / Role: $role_name / Instance Profile: $profile_name / 인라인 정책: $policy_name"
account="$(aws sts get-caller-identity --query Account --output text)"
echo "  AWS 계정: $account"
if [ "$assume_yes" != true ]; then
  read -r -p "AWS 계정 $account 에 적용합니다. 계속할까요? [y/N] " answer
  case "$answer" in
    y|Y|yes|YES) ;;
    *) echo "취소했습니다."; exit 1 ;;
  esac
fi

# 1) Role: 없으면 만든다 (신뢰 정책: ec2.amazonaws.com이 assume)
if aws iam get-role --role-name "$role_name" > /dev/null 2>&1; then
  echo "[setup-iam] Role 있음: $role_name"
else
  aws iam create-role --role-name "$role_name" \
    --assume-role-policy-document "$(cat "$trust_policy")" > /dev/null
  echo "[setup-iam] Role 생성: $role_name"
fi

# 2) 인라인 정책: 같은 이름이면 덮어쓰므로 다시 실행해도 안전하다
aws iam put-role-policy --role-name "$role_name" --policy-name "$policy_name" \
  --policy-document "$(cat "$tmp_policy")"
echo "[setup-iam] 정책 적용: $policy_name (s3://$bucket/images/* Put/Get, 버킷 ListBucket)"

# 3) Instance Profile: 없으면 만든다
if aws iam get-instance-profile --instance-profile-name "$profile_name" > /dev/null 2>&1; then
  echo "[setup-iam] Instance Profile 있음: $profile_name"
else
  aws iam create-instance-profile --instance-profile-name "$profile_name" > /dev/null
  echo "[setup-iam] Instance Profile 생성: $profile_name"
fi

# 4) Role을 Instance Profile에 넣는다 (프로파일당 Role 1개만 가능)
attached="$(aws iam get-instance-profile --instance-profile-name "$profile_name" \
  --query 'InstanceProfile.Roles[].RoleName' --output text)"
if [ "$attached" = "$role_name" ]; then
  echo "[setup-iam] Role이 이미 Instance Profile에 있음"
elif [ -n "$attached" ]; then
  echo "Instance Profile $profile_name 에 다른 Role($attached)이 있습니다. 직접 확인하세요." >&2
  exit 1
else
  aws iam add-role-to-instance-profile --instance-profile-name "$profile_name" --role-name "$role_name"
  echo "[setup-iam] Role을 Instance Profile에 추가"
fi

# 5) EC2에 붙이는 것은 하지 않는다. 명령만 안내한다 (콘솔: EC2 > 인스턴스 > 작업 > 보안 > IAM 역할 수정)
echo
echo "다음 단계: EC2 인스턴스에 Instance Profile을 붙입니다 (실행하지 않았습니다)."
echo "  aws ec2 associate-iam-instance-profile --instance-id <인스턴스 ID> --iam-instance-profile Name=$profile_name"
echo "  (이미 다른 프로파일이 붙어 있으면 replace-iam-instance-profile-association 또는 콘솔에서 교체)"
echo "[setup-iam] 완료"
