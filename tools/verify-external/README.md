# 외부 연동 확인 스크립트

키를 세팅한 뒤(R-91, 사용자) 외부 연동이 실제로 동작하는지 확인하는 `verify.sh`입니다. 키 세팅 순서와 연동별 채울 곳은 [05 §9 키 세팅 체크리스트](../../docs/restoration/05-external-integrations.md#9-키-세팅-체크리스트)를 봅니다.

## 언제 쓰나

1. 05 §9대로 `.env`·`secrets/`·`tools/fcm-test/firebase-config.js`를 채우고 바뀐 서비스를 다시 만든 뒤 (`docker compose up -d`)
2. main(VWorld·FCM)과 batch(Lost112)가 떠 있는 상태에서.

키가 없는 상태(지금)에서 실행해도 안전합니다: 외부 호출은 물론 main·batch로도 요청을 보내지 않고 "미설정"만 보고합니다.

## 사용법

레포 루트(어디서 실행해도 스크립트 위치 기준으로 레포를 찾습니다)에서, bash(Git Bash·Linux·macOS)로:

```sh
bash tools/verify-external/verify.sh                          # 설정된 연동만 확인
bash tools/verify-external/verify.sh --only vworld            # 확인 요청을 VWorld로 좁힘 (vworld|lost112|fcm, 여러 번 가능)
bash tools/verify-external/verify.sh --skip-lost112-collect   # Lost112는 설정 검사만 (수집 요청 없음)
bash tools/verify-external/verify.sh --fcm-phone 010-0000-0001   # FCM 테스트 발송까지
bash tools/verify-external/verify.sh --yes                    # 확인 프롬프트 없이
```

설정은 **`.env` 파일 기준**입니다 (`.env`를 source하지 않고 Compose 규칙대로 읽음). 셸 환경변수가 `.env`보다 우선하는 Compose 규칙은 반영하지 않습니다. 주소는 `http://localhost:{MAIN_HOST_PORT}`(기본 8080), `http://localhost:{BATCH_HOST_PORT}`(기본 8082)입니다. `.env`가 없으면 종료 코드 2.

## 동작

1. **설정 검사 (네트워크 없음)**: 연동마다 `미설정` / `설정 오류` / `설정됨`.
2. **확인 요청**: `설정됨`인 연동만, `--only`로 좁혔으면 그것만. 확인할 연동이 하나도 없으면 어떤 요청도 보내지 않습니다. 보내기 전에 무엇을 호출하는지 출력하고 `[y/N]`로 묻습니다(`--yes`로 생략). **터미널이 아니면(파이프·CI) 묻지 않고 요청을 보내지 않으며**, 해당 연동을 `확인 안 함`으로 표시합니다 — 진행하려면 `--yes`.
3. **요약**: 연동 | 상태 | 메모 표. `설정 오류`·`확인 실패`가 하나라도 있으면 종료 코드 1, 아니면 0 (미설정만 있어도 0).

| 연동 | 확인 요청 | 외부로 나가는 호출 |
|---|---|---|
| VWorld | main `GET /location/search?query=서울역…`, `GET /location/address?address=…` | main이 VWorld 호출 |
| Lost112 | batch `GET /search/total` → `POST /search/save` → `GET /search/total` | batch가 공공데이터포털 호출 (**트래픽 소모**, 아래) |
| FCM 서버 | `--fcm-phone`이 있을 때만: main `POST /members/login` → `POST /alarm/send-fcm/{memberId}` | main이 FCM 호출 |

스크립트가 외부 서비스에 직접 요청하지는 않습니다. 키·JWT·서비스계정 내용·`firebase-config.js` 내용은 출력하지 않습니다 (키는 "설정됨(길이 N)"까지).

## 상태의 뜻과 다음 행동

| 상태 | 뜻 | 다음 행동 |
|---|---|---|
| `미설정` | 키·파일이 없음 (정상: 아직 안 채움) | 05 §9에서 해당 행을 채움 |
| `설정 오류` | 값이 잘못됨. 예: `FCM_ENABLED=true`인데 `secrets/` 파일이 없음(**main이 기동하지 않음**), `FCM_ENABLED` 값이 boolean이 아님, 키 없이 `LOST112_COLLECT_ENABLED=true`, 테스트 페이지 설정의 필수 값이 비어 있음 | 메모대로 고치고 필요하면 `docker compose up -d` |
| `설정됨` | 설정 검사 통과. FCM 테스트 페이지는 이 스크립트가 요청으로 확인하지 않으므로 계속 `설정됨` | 브라우저에서 `tools/fcm-test`로 확인 |
| `확인 성공` | 요청이 성공 (VWorld: `response.status`가 `OK` 또는 `NOT_FOUND`) | – |
| `부분 성공` | Lost112 수집이 200이지만 일부 서비스가 error로 끝났거나 문서 수가 0 | 출력의 서비스 결과와 05 §3·§8 |
| `확인 실패` | 503(main·batch가 키를 못 읽음: `.env` 확인 후 `docker compose up -d main`/`batch`로 다시 만들기), 502(외부 서비스 오류·키 미등록·트래픽 초과), VWorld `response.status=ERROR`(`error.code`는 05 §5), 연결 실패(스택이 안 떠 있음), 시간 초과 | 메모와 해당 서비스 로그(`docker compose logs main`/`batch`) |
| `안내` | FCM 서버가 설정됨이지만 `--fcm-phone`이 없어 발송하지 않음 | 아래 FCM 절 |
| `건너뜀` | `--skip-lost112-collect` | – |
| `확인 안 함` | 설정됨이지만 `--only`로 빠졌거나, 확인 프롬프트를 통과하지 않음 | `--only`·`--yes` 조정 |
| `제외(추후, D-50)` | Naver 로그인은 1차 범위 밖이라 값이 있어도 확인하지 않음 | – |

AWS S3는 이 스크립트가 확인하지 않습니다 (요약에 한 줄 안내): 배포 시 [`infra/aws/README.md`](../../infra/aws/README.md).

## Lost112 수집과 트래픽

`POST /search/save`는 최근 `LOST112_COLLECT_DAYS`일(기본 30) 전체를 `LOST112_PAGE_SIZE`건(기본 1000)씩 페이지마다 받아 인덱싱합니다. 두 서비스(경찰청·포털기관) 모두 호출하므로 **포털기관 개발계정 일일 트래픽 한도 10,000건**을 쓰고 수분 걸릴 수 있습니다 (스크립트의 요청 제한 시간은 600초). 트래픽을 아끼려면 `--skip-lost112-collect`로 설정만 확인하거나, `.env`의 `LOST112_COLLECT_DAYS`를 줄이고 batch를 다시 만듭니다. 성공하면 수집 전후 문서 수를 보여 줍니다.

## FCM은 테스트 페이지와 함께

서버(main → FCM)는 이 스크립트로, 브라우저(토큰 발급·알림 수신)는 [`tools/fcm-test`](../fcm-test/README.md)로 확인합니다. 스크립트는 알림이 실제로 도착했는지 알 수 없습니다.

1. `secrets/firebase-adminsdk.json`, `.env`의 `FCM_ENABLED=true`, `tools/fcm-test/firebase-config.js`를 채우고 main을 다시 만듭니다 (05 §9).
2. 테스트 페이지(`python -m http.server 5500 -d tools/fcm-test` → `http://localhost:5500`)에서 로그인 → 권한 → 토큰 발급 → 토큰 저장까지 합니다. 이때 `verify.sh`를 `--fcm-phone` 없이 돌리면 `안내`가 나옵니다.
3. 토큰을 등록한 회원의 전화번호로 `--fcm-phone`을 줘서 다시 실행합니다 (main이 local 프로필이어야 하며, 시드 회원은 `010-0000-0001`). 로그인·발송 요청이 200이면 `확인 성공`이고, 브라우저 알림과 `docker compose logs main`(`FCM 발송 완료`)을 눈으로 확인합니다.
