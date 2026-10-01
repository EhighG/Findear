# 09. 배포 준비 · AWS 연동 키트

> 목표(P5): 실제 실행은 로컬. EC2 등 리소스만 확보하면 **값만 넣고 바로 배포**되는 상태를 갖춘다. HTTPS 없음(P6). AWS 비용은 사용자가 배포 시점에 판단(O-2).

## 1. 배포 흐름

```
git push master ──▶ GitHub Actions (images.yml) ──▶ GHCR: ghcr.io/ehighg/findear-{main,batch,match}:{sha,latest}
                                                          │ pull
EC2 (Ubuntu) : repo clone + .env + secrets/ ──▶ deploy.sh ──▶ docker compose -f compose.yml -f compose.prod.yml up -d
```
- 레포는 public이라 EC2에서 인증 없이 clone 가능. 이미지는 GHCR public 패키지 (첫 푸시 후 visibility 확인).
- EC2에서 빌드하지 않음 → 작은 인스턴스에서도 배포 가능.
- 롤백: `.env`의 `IMAGE_TAG`를 이전 커밋 SHA로 바꾸고 `deploy.sh` 재실행.
- 스키마: `flyway` one-shot이 배포 때마다 실행 (이미 적용된 버전은 건너뜀).
- Graviton(ARM) 인스턴스를 쓰려면 `images.yml`에서 buildx로 `linux/amd64,linux/arm64` 멀티 아키텍처 빌드.

## 2. GitHub Actions (R-60, R-61, R-65)

| 워크플로 | 트리거 | 내용 |
|---|---|---|
| `ci.yml` | PR, push (D-58: 자동, 테스트만) | JDK 17 + Gradle 캐시, main/batch/match `build`·`test` |
| `images.yml` | **수동(`workflow_dispatch`)만** — master push 자동 실행은 하지 않음(D-58, 배포를 결정할 때 실행) | `docker/login-action`(GITHUB_TOKEN), `docker/build-push-action`, `permissions: packages: write` |
| `deploy.yml` (선택) | workflow_dispatch | SSH로 EC2에서 `deploy.sh` 실행. 시크릿: `EC2_HOST`, `EC2_USER`, `EC2_SSH_KEY` |

포크 레포의 Actions는 켜져 있음(U-03, 2026-10-01 확인). 배포 흐름(§1)의 "git push master → images.yml"은 D-58에 따라 "Actions에서 images.yml 수동 실행"으로 읽는다.

## 3. 서버 준비 (R-62, R-63)

- `infra/deploy/init-host.sh` (Ubuntu 24.04 기준): Docker Engine + compose plugin 설치, 사용자 docker 그룹 추가, `sysctl vm.max_map_count=262144` 영구 설정, (메모리 작으면) swap 파일, 앱 디렉토리 생성·clone, `.env.example` → `.env` 복사 안내.
- `infra/deploy/deploy.sh`: `git pull` → `docker compose -f compose.yml -f compose.prod.yml pull` → `up -d` → `ps`로 healthy 확인.
- 보안그룹: 22(관리자 IP만), 80(main). DB·ES·Redis·Prometheus·Grafana 포트는 열지 않음. Grafana는 SSH 터널로 접근.
- Docker 게시 포트는 UFW를 우회하므로 방화벽은 보안그룹으로 관리.
- 인스턴스 크기: 메모리 제한 기본값(최소 사양) 합계 약 3.6GB + OS → 4GB급은 swap 2GB 이상이 있어야 기동 가능한 수준, 여유 있게는 8GB급 (O-2). 배포 서버에서 제한을 올리려면 `.env`의 `*_MEM_LIMIT`만 바꾼다.
- 1차 작업의 검증 범위 (D-41): `compose.prod.yml`은 `docker compose -f compose.yml -f compose.prod.yml config --quiet`, 스크립트는 `bash -n`까지. EC2에서의 실행 확인은 배포할 때 사용자가 한다.

## 4. AWS S3 연동 키트 (R-64)

사용자가 나중에 한 곳에서 보고 연동할 수 있도록 `infra/aws/`에 모읍니다. 이 단계에서는 **AWS 리소스를 만들지 않고, 실제 값도 넣지 않습니다.**

| 파일 | 내용 |
|---|---|
| `infra/aws/README.md` | 연동 체크리스트: ① 버킷 이름·리전 결정 ② `setup-s3.sh` 실행 ③ IAM 정책·EC2 Role 생성 후 인스턴스에 연결 ④ `.env`의 STORAGE 값 변경 ⑤ 확인 명령(presign → PUT → GET) ⑥ (선택) CloudFront |
| `infra/aws/s3/setup-s3.sh` | AWS CLI: `s3api create-bucket`(LocationConstraint=ap-northeast-2), `put-public-access-block`(정책 기반 공개만 허용, ACL 차단 유지), `put-bucket-cors`, `put-bucket-policy`. 로컬 `storage-init`과 같은 명령 사용 |
| `infra/aws/s3/cors.json` | `PUT`, `GET`, `HEAD` 허용, origin은 프론트 주소, `ETag` 노출 |
| `infra/aws/s3/bucket-policy.json` | `s3:GetObject`를 `arn:aws:s3:::{bucket}/images/*`에만 공개 |
| `infra/aws/iam/findear-app-policy.json` | 버킷 대상 `s3:PutObject`, `s3:GetObject`, `s3:DeleteObject` (+ 필요 시 `s3:ListBucket`) 최소 권한 |
| `infra/aws/iam/ec2-trust-policy.json`, `setup-iam.sh` | EC2용 Role·Instance Profile 생성 및 정책 연결 |

전환 시 바꿀 `.env` 값: `STORAGE_ENDPOINT=`(빈 값), `STORAGE_PUBLIC_ENDPOINT=`(빈 값), `STORAGE_PATH_STYLE=false`, `STORAGE_BUCKET={버킷}`, `STORAGE_PUBLIC_BASE_URL=https://{버킷}.s3.ap-northeast-2.amazonaws.com`, `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY` 삭제(EC2 Role 사용). 코드 변경은 없어야 합니다.

R-13에서 만든 `infra/seaweedfs/storage-init.sh`는 `STORAGE_ENDPOINT`를 비우면 AWS 기본 엔드포인트로 같은 명령(버킷 생성 → CORS → `images/*` 공개 읽기 버킷 정책)을 실행한다. `setup-s3.sh`는 이 스크립트를 재사용하되, AWS에서는 새 버킷에 Block Public Access가 기본으로 켜져 있으므로 **정책 적용 전에 `put-public-access-block`(BlockPublicPolicy·RestrictPublicBuckets 해제, ACL 차단은 유지)**을 넣는다 (AWS 전용 단계, D-45).

검증 범위 (D-41): 버킷 생성·CORS·버킷 정책 명령은 로컬 SeaweedFS(`storage-init`)에서 같은 형식으로 동작하는지 확인하고, AWS 전용 명령(Public Access Block, 버킷 정책, IAM)과 정책 JSON은 `bash -n`·JSON 문법 검사까지 합니다. AWS에 연결해야 하는 확인(README의 ⑤ 포함)은 사용자가 배포할 때(U-08) 합니다.

## 5. 배포 환경 제약 (기록)

- HTTP 도메인에서는 브라우저 웹푸시(FCM)가 동작하지 않음 → O-7.
- Naver 로그인 Callback URL, CORS origin, `NAVER_REDIRECT_URI`를 배포 주소로 바꿔야 함.
