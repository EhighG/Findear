# AWS S3 연동 키트

배포(U-08)를 결정했을 때 AWS 쪽에서 해야 할 일을 한 곳에 모은 것입니다. 설계는 [09 §4](../../docs/restoration/09-deploy-and-aws.md#4-aws-s3-연동-키트-r-64), [06 §4](../../docs/restoration/06-db-and-config.md)입니다.

> **1차 복구 작업에서는 AWS에 연결하지 않았습니다 (D-38, D-41).** 이 키트의 스크립트는 AWS를 한 번도 실행하지 않았고, 버킷·역할을 만들지도 않았으며 실제 값도 들어 있지 않습니다. 검증은 `bash -n`·shellcheck·JSON 문법 검사와, 같은 버킷·CORS·정책 명령을 로컬 SeaweedFS에서 돌리는 리허설까지입니다. AWS 전용 단계(Public Access Block, IAM, AWS가 공개 정책을 실제로 받아들이는지)는 처음 실행할 때 사용자가 확인합니다.
> 비용은 사용자 판단입니다 (O-2). S3 저장·요청·전송 요금이 생깁니다.

## 파일

| 파일 | 내용 |
|---|---|
| `s3/setup-s3.sh` | 버킷 생성 → Public Access Block 조정(AWS 전용) → CORS·`images/*` 공개 읽기 정책 → 결과 출력 |
| `iam/findear-app-policy.json` | 서버 Role용 최소 권한 템플릿 (`__BUCKET__` 자리표시자) |
| `iam/ec2-trust-policy.json` | EC2(`ec2.amazonaws.com`)가 Role을 쓰게 하는 신뢰 정책 |
| `iam/setup-iam.sh` | Role·인라인 정책·Instance Profile 생성 (`--render-only`는 AWS 호출 없이 정책만 출력) |

CORS와 버킷 정책(공개 읽기)의 JSON은 별도 파일이 아니라 [`infra/seaweedfs/storage-init.sh`](../seaweedfs/storage-init.sh)가 환경변수로 만듭니다. 로컬 SeaweedFS와 AWS가 같은 기준을 쓰도록 `setup-s3.sh`가 그 스크립트를 그대로 실행합니다.

## 체크리스트

필요한 것: AWS 계정, 관리자 권한 자격증명이 설정된 AWS CLI v2 (`aws configure`, `AWS_PROFILE`, SSO 등. **앱용 액세스 키는 만들지 않습니다**), 프론트 주소(CORS origin), 배포할 EC2.

실행 환경: 스크립트는 bash와 `/tmp`를 쓰므로 **Linux·macOS 셸(EC2, AWS CloudShell 포함)**에서 실행합니다. Windows용 `aws.exe`로는 `storage-init.sh`의 `file:///tmp/…` 경로가 맞지 않을 수 있습니다.

### ① 버킷 이름·리전 결정
- 버킷 이름: 전 세계에서 유일한 소문자·숫자·`.`·`-` 3~63자 (예: `findear-images-<식별자>`).
- 리전: 기본 `ap-northeast-2`(서울). EC2와 같은 리전을 권장합니다.
- CORS origin: 프론트 배포 주소(`https://…`). 쉼표로 여러 개.

### ② 버킷 준비
```bash
export STORAGE_BUCKET=<버킷>
export AWS_REGION=ap-northeast-2
export CORS_ALLOWED_ORIGINS=https://<프론트 주소>
bash infra/aws/s3/setup-s3.sh        # "AWS 계정 <ID>에 적용합니다" 확인 후 진행. 생략은 --yes
```
하는 일 (다시 실행해도 같은 결과):
1. 버킷이 없으면 생성 (`us-east-1`이 아니면 `LocationConstraint`). 새 버킷은 Object Ownership 기본값이 BucketOwnerEnforced라 ACL이 꺼져 있습니다.
2. `put-public-access-block`: `BlockPublicAcls=true, IgnorePublicAcls=true, BlockPublicPolicy=false, RestrictPublicBuckets=false`. 새 버킷은 네 설정이 모두 켜져 있어 공개 읽기 **버킷 정책**이 거부되므로 정책 관련 둘만 끄고, ACL 기반 공개는 계속 막습니다.
3. `storage-init.sh`: CORS(`PUT`·`GET`·`HEAD`, `ETag` 노출)와 `images/*`의 `s3:GetObject`만 공개하는 버킷 정책. 나머지 경로·쓰기·목록은 자격증명이 필요합니다.
4. `get-bucket-cors`, `get-bucket-policy`, `get-public-access-block` 출력.

> 계정 수준 Block Public Access나 조직 정책이 켜져 있으면 버킷 설정을 풀어도 공개 정책이 거부될 수 있습니다(가장 엄격한 설정이 적용됨). 그 경우 `put-bucket-policy`가 AccessDenied로 실패하니 계정 설정을 확인하세요.

### ③ IAM Role·Instance Profile, EC2에 연결
```bash
STORAGE_BUCKET=<버킷> bash infra/aws/iam/setup-iam.sh --render-only   # 먼저 정책 내용 검토 (AWS 호출 없음)
STORAGE_BUCKET=<버킷> bash infra/aws/iam/setup-iam.sh                 # Role findear-app, 인라인 정책 findear-app-s3, Instance Profile findear-app
```
이름을 바꾸려면 `ROLE_NAME`, `INSTANCE_PROFILE_NAME`, `POLICY_NAME` 환경변수를 씁니다. 끝에 출력되는 연결 명령(스크립트는 실행하지 않음):
```bash
aws ec2 associate-iam-instance-profile --instance-id <인스턴스 ID> --iam-instance-profile Name=findear-app
```
콘솔에서는 EC2 > 인스턴스 > 작업 > 보안 > IAM 역할 수정입니다. 연결하는 사람에게는 `ec2:AssociateIamInstanceProfile`과 해당 Role에 대한 `iam:PassRole`이 필요합니다.

**컨테이너에서 Role 자격증명 받기 (IMDS hop limit)**: main은 EC2 위 Docker 브리지 네트워크의 컨테이너라, Instance Profile 자격증명을 인스턴스 메타데이터(IMDSv2)에서 받을 때 컨테이너까지 한 홉이 더 있습니다. PUT 응답 hop limit이 1이면 SDK가 자격증명을 받지 못하거나 늦게 받아 presign·HeadObject가 실패할 수 있으므로 **hop limit을 2로** 둡니다 (EC2 사용자 가이드 "Instance metadata access considerations" — "In a container environment, consider … increasing the hop limit to 2"):
```bash
aws ec2 describe-instances --instance-id <인스턴스 ID> --query 'Reservations[].Instances[].MetadataOptions'   # 현재 값 확인
aws ec2 modify-instance-metadata-options --instance-id <인스턴스 ID> --http-tokens required --http-put-response-hop-limit 2
```

### ④ 서버 `.env`의 STORAGE 값 변경 (코드 변경 없음)
```
STORAGE_PUBLIC_ENDPOINT=                 # 빈 값 (compose.prod.yml이 main의 STORAGE_ENDPOINT·STORAGE_PUBLIC_ENDPOINT를 빈 값으로 고정하지만 .env도 맞춘다)
STORAGE_PATH_STYLE=false
STORAGE_BUCKET=<버킷>
STORAGE_PUBLIC_BASE_URL=https://<버킷>.s3.<리전>.amazonaws.com
AWS_REGION=<리전>
AWS_ACCESS_KEY_ID=                       # 비움 (EC2 Role의 임시 자격증명을 SDK 기본 체인이 사용)
AWS_SECRET_ACCESS_KEY=                   # 비움
CORS_ALLOWED_ORIGINS=https://<프론트 주소>
```
그 뒤 `infra/deploy/deploy.sh`(또는 `docker compose -f compose.yml -f compose.prod.yml up -d`)로 다시 띄웁니다. 배포 compose에는 SeaweedFS가 없습니다.

### ⑤ 확인 (서버에서, 07/08 R-90의 3단계와 같은 흐름)
1. 로그인해서 액세스 토큰을 받는다.
2. `POST /images/presign` (본문 `{"contentType":"image/jpeg","contentLength":<바이트>}`) → 응답의 `key`, `uploadUrl`, `url`(업로드 뒤 조회용 공개 URL), `headers`.
3. 응답의 `headers`를 그대로 붙여 업로드: `curl -X PUT -H '<headers의 각 항목>' --upload-file ./sample.jpg '<uploadUrl>'` → 200.
4. 응답의 `url`로 익명 GET → 200 (`https://<버킷>.s3.<리전>.amazonaws.com/images/…`).
5. 게시글 등록 API에 `key`를 `imgKeys`로 넘겨 등록 → 성공(내부에서 `HeadObject`로 존재 확인). 존재하지 않는 key를 넘기면 "업로드되지 않은 이미지"로 거절되어야 합니다 (저장소 오류가 나면 `s3:ListBucket` 권한을 확인).
6. 브라우저에서 프론트 origin으로 PUT이 CORS에 막히지 않는지 확인.

### ⑥ (선택) CloudFront
공개 읽기를 CloudFront로 돌리려면 배포를 만들고 `STORAGE_PUBLIC_BASE_URL`을 CloudFront 주소로 바꿉니다. 이때는 OAC로 S3를 CloudFront에만 열면 되므로 버킷 정책의 공개 읽기·Public Access Block 조정이 필요 없어집니다. 이 키트는 CloudFront를 다루지 않습니다.

## 권한 표 (main이 S3에 하는 일)

서버 Role(`iam/findear-app-policy.json`)이 가지는 권한과 쓰는 곳입니다. 코드 위치는 `main/src/main/java/com/findear/main/storage/`.

| 동작 | S3 API | 필요한 권한 | 리소스 |
|---|---|---|---|
| presigned PUT URL 발급 (`POST /images/presign`) | 서버는 서명만 함(S3 호출 없음). 브라우저가 URL로 `PutObject` | `s3:PutObject` — **서명한 자격증명(서버 Role)에 있어야** 브라우저 업로드가 성공 | `arn:aws:s3:::<버킷>/images/*` |
| 등록 때 key 존재 확인 | `HeadObject` | `s3:GetObject` | `arn:aws:s3:::<버킷>/images/*` |
| 없는 key의 HeadObject가 404를 받도록 | (같은 `HeadObject`) | `s3:ListBucket` | `arn:aws:s3:::<버킷>` |
| 이미지 조회(익명) | `GetObject` | 버킷 정책의 공개 읽기 (Role과 무관) | `images/*` |

- `s3:ListBucket`: 공식 문서(HeadObject Permissions)에 "없는 객체를 요청하면 `ListBucket`이 있을 때 404, 없을 때 403"이라고 되어 있습니다. 현재 구현은 404를 "업로드되지 않음"으로, 403을 저장소 오류로 처리하므로 필요합니다.
- **`s3:ListBucket`에 `s3:prefix` 조건은 붙이지 않았습니다.** `s3:prefix`는 목록 조회 요청(`ListObjects`의 `prefix` 파라미터)에 대한 조건 키이고, HeadObject 요청에는 prefix 파라미터가 없어 조건이 어떻게 평가되는지 공식 문서에서 확인되지 않았습니다. 조건 때문에 404가 403으로 바뀌면 등록이 깨지므로, 확인되지 않은 조건은 넣지 않고 버킷 전체 `ListBucket`(키 이름 목록 조회 가능)을 허용했습니다. 이 버킷에는 `images/*`만 있고 그 URL은 어차피 공개 읽기라 추가로 노출되는 것은 키 이름 목록 정도입니다. 조건을 붙이고 싶으면 배포 후 ⑤의 5번(없는 key가 정상적으로 거절되는지)으로 확인한 뒤 붙이세요.
- **`s3:DeleteObject`는 넣지 않았습니다.** main은 S3 객체를 삭제하지 않습니다(수정·삭제로 떨어진 옛 객체와 쓰이지 않은 객체가 남는 한계는 1차 이후, 08 "1차 목표 이후"). 정리 기능을 만들면 정책에 `s3:DeleteObject`(`images/*`)를 추가하세요.

## 로컬 리허설 (AWS 호출 없음)

`STORAGE_ENDPOINT`를 넣으면 `setup-s3.sh`가 그 엔드포인트로만 보내고 AWS 전용 단계(계정 확인 프롬프트, Public Access Block)를 건너뜁니다. 로컬 SeaweedFS로 같은 버킷·CORS·정책 명령이 동작하는지 확인하는 방법입니다 (레포 루트, Git Bash):
```bash
docker compose up -d seaweedfs          # healthy 대기. .env에 COMPOSE_PROFILES가 있어도 서비스 이름을 지정한다
MSYS_NO_PATHCONV=1 docker run --rm --network findear --entrypoint bash \
  -v "$(pwd -W)/infra:/infra:ro" \
  -e AWS_ACCESS_KEY_ID=<.env의 AWS_ACCESS_KEY_ID> -e AWS_SECRET_ACCESS_KEY=<.env의 AWS_SECRET_ACCESS_KEY> \
  -e STORAGE_BUCKET=findear-kit-rehearsal -e STORAGE_ENDPOINT=http://seaweedfs:8333 \
  -e CORS_ALLOWED_ORIGINS=http://localhost:5173 \
  amazon/aws-cli:2.37.5 /infra/aws/s3/setup-s3.sh
# 정리 (빈 버킷이면 삭제 가능)
MSYS_NO_PATHCONV=1 docker run --rm --network findear \
  -e AWS_ACCESS_KEY_ID=<…> -e AWS_SECRET_ACCESS_KEY=<…> -e AWS_DEFAULT_REGION=ap-northeast-2 \
  amazon/aws-cli:2.37.5 --endpoint-url http://seaweedfs:8333 s3api delete-bucket --bucket findear-kit-rehearsal
docker compose down                      # 로컬 개발 데이터가 든 볼륨은 지우지 않는다
```
Linux/macOS에서는 `$(pwd -W)` 대신 `$(pwd)`를 쓰고 `MSYS_NO_PATHCONV`는 필요 없습니다. 한 번 더 실행하면 "버킷 있음"으로 건너뛰고 같은 결과가 나옵니다.

IAM 정책은 AWS 없이 `STORAGE_BUCKET=example-bucket bash infra/aws/iam/setup-iam.sh --render-only | python -m json.tool`로 확인합니다.

## 확인한 AWS 공식 문서 (2026-10-01, CLI 레퍼런스는 AWS CLI 2.37.7 기준)

- S3 API `HeadObject` Permissions: https://docs.aws.amazon.com/AmazonS3/latest/API/API_HeadObject.html
- CLI `s3api`: [create-bucket](https://docs.aws.amazon.com/cli/latest/reference/s3api/create-bucket.html) · [head-bucket](https://docs.aws.amazon.com/cli/latest/reference/s3api/head-bucket.html) · [put-public-access-block](https://docs.aws.amazon.com/cli/latest/reference/s3api/put-public-access-block.html) · [put-bucket-cors](https://docs.aws.amazon.com/cli/latest/reference/s3api/put-bucket-cors.html) · [put-bucket-policy](https://docs.aws.amazon.com/cli/latest/reference/s3api/put-bucket-policy.html)
- S3 Block Public Access: https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-control-block-public-access.html
- S3 Object Ownership (기본 BucketOwnerEnforced, ACL 비활성): https://docs.aws.amazon.com/AmazonS3/latest/userguide/about-object-ownership.html
- IAM CLI: [create-role](https://docs.aws.amazon.com/cli/latest/reference/iam/create-role.html) · [put-role-policy](https://docs.aws.amazon.com/cli/latest/reference/iam/put-role-policy.html) · [create-instance-profile](https://docs.aws.amazon.com/cli/latest/reference/iam/create-instance-profile.html) · [add-role-to-instance-profile](https://docs.aws.amazon.com/cli/latest/reference/iam/add-role-to-instance-profile.html) · EC2 [associate-iam-instance-profile](https://docs.aws.amazon.com/cli/latest/reference/ec2/associate-iam-instance-profile.html) · STS [get-caller-identity](https://docs.aws.amazon.com/cli/latest/reference/sts/get-caller-identity.html)
- EC2용 IAM Role (신뢰 정책 `ec2.amazonaws.com`, Instance Profile): https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_use_switch-role-ec2.html
