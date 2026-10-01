# 09. 배포 준비 · AWS 연동 키트

> 목표(P5): 실제 실행은 로컬. EC2 등 리소스만 확보하면 **값만 넣고 바로 배포**되는 상태를 갖춘다. HTTPS 없음(P6). AWS 비용은 사용자가 배포 시점에 판단(O-2).

## 1. 배포 흐름

```
Actions에서 images.yml 수동 실행(master) ──▶ GHCR: ghcr.io/ehighg/findear-{main,batch,match}:{sha,latest}
                                             │ pull
EC2 (Ubuntu) : repo clone + .env + secrets/ ──▶ deploy.sh ──▶ docker compose -f compose.yml -f compose.prod.yml up -d
```
- 레포는 public이라 EC2에서 인증 없이 clone 가능. 이미지는 GHCR public 패키지 (첫 푸시 후 visibility 확인).
- EC2에서 빌드하지 않음 → 작은 인스턴스에서도 배포 가능.
- 롤백: `infra/deploy/deploy.sh --tag <이전 커밋 SHA>` (이번 실행에만 `IMAGE_TAG`를 셸 환경변수로 덮어씀) 또는 `.env`의 `IMAGE_TAG`를 바꾸고 재실행. 이미지만 돌아가고 compose 파일은 현재 것이므로 구성까지 되돌리려면 `git checkout <SHA>` 후 `deploy.sh --no-git --tag <SHA>`.
- 스키마: `flyway` one-shot이 배포 때마다 실행 (이미 적용된 버전은 건너뜀).
- Graviton(ARM) 인스턴스를 쓰려면 `images.yml`에서 buildx로 `linux/amd64,linux/arm64` 멀티 아키텍처 빌드.

## 2. GitHub Actions (R-60, R-61, R-65)

| 워크플로 | 트리거 | 내용 |
|---|---|---|
| `ci.yml` | PR, push (D-58: 자동, 테스트만) | JDK 17 + Gradle 캐시, main/batch/match `build`·`test` |
| `images.yml` | **수동(`workflow_dispatch`)만** — master push 자동 실행은 하지 않음(D-58, 배포를 결정할 때 실행). 입력 `platforms`(`linux/amd64` 기본 / `linux/amd64,linux/arm64`), master에서만 실행 | `docker/login-action`(GITHUB_TOKEN), `docker/metadata-action`, `docker/build-push-action`(모듈별 matrix, 태그 `latest`·전체 커밋 SHA), `permissions: packages: write` |
| `deploy.yml` (선택) | workflow_dispatch | SSH로 EC2에서 `deploy.sh` 실행. 시크릿: `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY` |

포크 레포의 Actions는 켜져 있음(U-03, 2026-10-01 확인).

## 3. 서버 준비 (R-62, R-63)

- `infra/deploy/init-host.sh` (Ubuntu 24.04 기준, root로 실행, 다시 실행해도 같은 결과): `sudo bash infra/deploy/init-host.sh [--user <이름>] [--app-dir /opt/findear] [--repo <URL>] [--branch master] [--swap-size 2G]`. 레포가 아직 없으면 raw URL(`https://raw.githubusercontent.com/EhighG/Findear/master/infra/deploy/init-host.sh`)로 받아 실행한다. 단계: ① Ubuntu·root 확인 ② Docker Engine + compose plugin(공식 apt 저장소 방식, 이미 `docker compose`가 되면 건너뜀. compose 최소 버전 검사는 하지 않고 `deploy.sh --check`가 `compose config`로 확인) ③ `--user`를 docker 그룹에 추가(재로그인 필요) ④ `vm.max_map_count` 영구 설정(`/etc/sysctl.d/99-findear.conf`, 값은 Elasticsearch 문서가 현재 요구하는 1048576. 현재 값이 이미 그보다 크면 낮추지 않고 그 값을 파일에도 써서 재부팅 뒤에도 유지) ⑤ 활성 swap이 없으면 `/swapfile`(`--swap-size 0`이면 생략) ⑥ 앱 디렉토리에 `git clone`(이미 있으면 건너뜀, pull은 하지 않음) ⑦ `.env`(`.env.example` 복사, 600)·`secrets/`(소유자 `--user`, 그룹 gid 10001, 모드 2750(setgid) — 이미 있어도 매번 맞춤. compose.yml이 디렉토리째 마운트하고 main 컨테이너가 uid/gid 10001로 실행되므로 700이면 FCM 파일을 못 읽는다. FCM 서비스계정 파일은 `secrets/firebase-adminsdk.json`에 두고 `chmod 640` — setgid라 안에서 만든 파일은 그룹 10001을 받고, 다른 곳에서 옮겨 온 파일은 `sudo chgrp 10001` 필요(배포 사용자는 그룹 10001에 속하지 않음)) ⑧ 다음 할 일 안내.
- `infra/deploy/deploy.sh` (일반 사용자, 레포 루트에서 `compose.yml + compose.prod.yml`만 사용): `[--check] [--tag <IMAGE_TAG>] [--no-git]`. 순서: `git pull --ff-only`(작업 트리에 변경이 있으면 멈춤. pull로 HEAD가 바뀌면 `--no-git`을 붙여 새 deploy.sh를 처음부터 한 번 다시 실행) → `.env` 검사 → `compose config --quiet` → 사용할 이미지 출력 → `compose pull` → `up -d --remove-orphans --wait`(최대 600초) → `ps`·이미지·롤백 안내. `.env`는 `source`하지 않고 `KEY=값` 줄을 Compose 규칙에 맞춰 읽는다(따옴표 값은 닫는 따옴표까지, 그 뒤 ` # 주석` 무시, `export` 접두사·`=` 앞뒤 공백 허용). `up --wait`는 전체 스택에서 flyway가 성공으로 끝날 때까지 기다리고 실패하면 up이 실패한다(서비스 일부만 지정하면 이 의존이 빠지므로 전체를 올린다).
  - `.env` 검사 오류(모두 모아서 출력하고 exit 1): `.env` 없음 / `.env.example` 예시 값 그대로인 `JWT_SECRET`·`MYSQL_PASSWORD`·`MYSQL_ROOT_PASSWORD`·`MYSQL_EXPORTER_PASSWORD`·`GRAFANA_ADMIN_PASSWORD`·`AWS_ACCESS_KEY_ID`·`AWS_SECRET_ACCESS_KEY` / 비어 있는 `REDIS_PASSWORD`·`ELASTIC_PASSWORD`·`JWT_SECRET`·DB·Grafana 비밀번호·`STORAGE_BUCKET`·`STORAGE_PUBLIC_BASE_URL` / `STORAGE_PUBLIC_BASE_URL`의 localhost·127.0.0.1 / 비밀번호 값의 `$`·`'`·`\` / `FCM_ENABLED=true`인데 `secrets/` 파일 없음. 경고(계속 진행): `STORAGE_PATH_STYLE=true`, `AWS_ACCESS_KEY_ID` 값 있음, `SPRING_PROFILES_ACTIVE`가 prod 아님, `JWT_SECRET`이 32바이트 미만.
  - `--check`: 위 `.env` 검사·`compose config`·이미지 출력까지만 하고 끝낸다(git·pull·up 없음, 서버 밖에서도 실행 가능). `--tag`는 셸 환경변수 `IMAGE_TAG`로 넘겨 `.env`보다 우선한다(Compose 변수 우선순위).
- 보안그룹: 22(관리자 IP만), 80(main). DB·ES·Redis·Prometheus·Grafana 포트는 열지 않음. Grafana는 SSH 터널로 접근. **node-exporter는 호스트 네트워크라 9100이 호스트에 열리므로 보안그룹에서 열지 않는다.**
- `compose.prod.yml`이 하는 일 (R-62): GHCR 이미지(build 없음), 스토리지는 AWS S3만(seaweedfs·storage-init 제외, `STORAGE_ENDPOINT`·`STORAGE_PUBLIC_ENDPOINT` 빈 값), 프로필 `prod` 고정(D-60), main만 `80:8080`·Prometheus/Grafana는 `127.0.0.1`, Redis·ES 비밀번호(`REDIS_PASSWORD`·`ELASTIC_PASSWORD` 필수, 비면 compose가 오류), 로그 로테이션·`restart: unless-stopped`, node-exporter와 Grafana `host/` 대시보드(Node Exporter Full). 서버 `.env`에서 채울 값은 `compose.prod.yml` 머리 주석에 있다.
- Docker 게시 포트는 UFW를 우회하므로 방화벽은 보안그룹으로 관리.
- 인스턴스 크기: 메모리 제한 기본값(최소 사양) 합계 약 3.9GB + OS → 4GB급은 swap 2GB 이상이 있어야 기동 가능한 수준, 여유 있게는 8GB급 (O-2). 배포 서버에서 제한을 올리려면 `.env`의 `*_MEM_LIMIT`만 바꾼다.
- 1차 작업의 검증 범위 (D-41): `compose.prod.yml`은 `docker compose -f compose.yml -f compose.prod.yml config --quiet`, 스크립트는 `bash -n`까지. EC2에서의 실행 확인은 배포할 때 사용자가 한다.

## 4. AWS S3 연동 키트 (R-64)

사용자가 나중에 한 곳에서 보고 연동할 수 있도록 `infra/aws/`에 모읍니다. 이 단계에서는 **AWS 리소스를 만들지 않고, 실제 값도 넣지 않습니다.**

| 파일 | 내용 |
|---|---|
| `infra/aws/README.md` | 연동 체크리스트: ① 버킷 이름·리전 결정 ② `setup-s3.sh` 실행 ③ `setup-iam.sh` 실행 후 EC2에 Instance Profile 연결 ④ `.env`의 STORAGE 값 변경 ⑤ 확인 명령(presign → PUT → GET) ⑥ (선택) CloudFront. 권한 표, 로컬 리허설 방법, 확인한 공식 문서 목록 |
| `infra/aws/s3/setup-s3.sh` | 버킷 생성(`head-bucket` → `create-bucket`, LocationConstraint) → `put-public-access-block`(정책 기반 공개만 허용, ACL 차단 유지, **AWS 전용**) → `infra/seaweedfs/storage-init.sh` 그대로 실행(CORS·`images/*` 공개 읽기 정책) → `get-bucket-cors`·`get-bucket-policy`·`get-public-access-block` 출력. 실제 AWS에서는 `sts get-caller-identity` 계정 확인 프롬프트(`--yes`로 생략). **`STORAGE_ENDPOINT`를 넣으면 로컬 리허설 모드**: 그 엔드포인트(SeaweedFS)로만 보내고 AWS 전용 단계를 건너뜀 |
| `infra/aws/iam/findear-app-policy.json` | 서버 Role 최소 권한 템플릿(`__BUCKET__` 자리표시자): `s3:PutObject`·`s3:GetObject` → `{bucket}/images/*`, `s3:ListBucket` → 버킷. **`s3:DeleteObject`는 넣지 않음**: main은 객체를 삭제하지 않는다(고아 객체 정리는 1차 이후, 만들 때 추가). `ListBucket`이 없으면 없는 key의 `HeadObject`가 404가 아닌 403이라 필요하고, `s3:prefix` 조건은 HeadObject에 대한 평가가 공식 문서에 없어 붙이지 않음 |
| `infra/aws/iam/ec2-trust-policy.json`, `setup-iam.sh` | EC2용 신뢰 정책. `setup-iam.sh`: Role·인라인 정책(버킷 이름 치환)·Instance Profile 생성, Role 연결, EC2에 붙이는 명령은 출력만. `--render-only`는 AWS 호출 없이 치환된 정책 JSON만 출력 |

CORS(`PUT`·`GET`·`HEAD`, `ETag` 노출, origin은 프론트 주소)와 버킷 정책(`s3:GetObject`를 `arn:aws:s3:::{bucket}/images/*`에만 공개)은 `cors.json`·`bucket-policy.json` 같은 별도 파일로 두지 않는다. `storage-init.sh`가 환경변수(`STORAGE_BUCKET`, `CORS_ALLOWED_ORIGINS`)로 만들고, `setup-s3.sh`가 그것을 재사용하므로 기준이 한 곳이다.

전환 시 바꿀 `.env` 값: `STORAGE_ENDPOINT=`(빈 값, 배포 compose가 고정), `STORAGE_PUBLIC_ENDPOINT=`(빈 값), `STORAGE_PATH_STYLE=false`, `STORAGE_BUCKET={버킷}`, `STORAGE_PUBLIC_BASE_URL=https://{버킷}.s3.ap-northeast-2.amazonaws.com`, `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` 삭제(EC2 Role 사용). 코드 변경은 없어야 합니다.

R-13에서 만든 `infra/seaweedfs/storage-init.sh`는 `STORAGE_ENDPOINT`를 비우면 AWS 기본 엔드포인트로 같은 명령(버킷 생성 → CORS → `images/*` 공개 읽기 버킷 정책)을 실행한다. `setup-s3.sh`는 이 스크립트를 재사용하되, AWS에서는 새 버킷에 Block Public Access가 기본으로 켜져 있으므로 **정책 적용 전에 `put-public-access-block`(BlockPublicPolicy·RestrictPublicBuckets 해제, ACL 차단은 유지)**을 넣는다 (AWS 전용 단계, D-45).

검증 범위 (D-41, R-64에서 실제로 한 것): 스크립트 두 개는 `bash -n`과 shellcheck(경고 0건), 정책 JSON은 `python -m json.tool`, `setup-iam.sh --render-only`(버킷 이름 형식 검사 포함)로 확인했다. 버킷 생성·CORS·`images/*` 공개 정책 명령은 로컬 SeaweedFS에서 `setup-s3.sh`를 리허설 모드(`STORAGE_ENDPOINT` 지정, aws-cli 컨테이너)로 실행해 확인했다(재실행 포함). **AWS 전용 부분**(Public Access Block 호출, AWS가 공개 정책을 받아들이는지, IAM 생성, `sts` 계정 확인, 정책의 `ListBucket`이 404를 주는지)은 AWS를 호출하지 않으므로 확인하지 않았다. AWS에 연결해야 하는 확인(README의 ⑤ 포함)은 사용자가 배포할 때(U-08) 한다.

## 5. 배포 환경 제약 (기록)

- HTTP 도메인에서는 브라우저 웹푸시(FCM)가 동작하지 않음 → O-7.
- Naver 로그인 Callback URL, CORS origin, `NAVER_REDIRECT_URI`를 배포 주소로 바꿔야 함.
