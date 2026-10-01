# FCM 웹푸시 테스트 페이지

키 세팅 후(R-91) 브라우저에서 FCM 웹푸시를 확인하는 정적 페이지입니다. 빌드 도구 없이 Firebase JS SDK(CDN, 버전은 `sdk-version.js`)만 씁니다. 설정 파일이 없으면 Firebase를 불러오지도 초기화하지도 않고 설정 안내만 보여 줍니다.

## 준비물

1. U-04 ([05 §2](../../docs/restoration/05-external-integrations.md#2-firebase-cloud-messaging-웹푸시)): Firebase 새 프로젝트, 웹 앱의 `firebaseConfig`, 웹 푸시 인증서의 VAPID 공개키, 서비스 계정 JSON.
2. `firebase-config.example.js`를 `firebase-config.js`로 복사해 값을 채웁니다 (`self.FINDEAR_FCM_CONFIG`). 이 파일은 git에서 제외됩니다.
3. 서버 쪽: 서비스 계정 JSON을 `secrets/firebase-adminsdk.json`에 두고 `.env`에 `FCM_ENABLED=true`. main은 **local 프로필**로 실행합니다 (`POST /members/login`, `POST /alarm/send-fcm/{memberId}`가 local 전용). 시드 회원은 `010-0000-0001`(NORMAL), `010-0000-0002`(MANAGER).

## 실행

레포 루트에서:

```sh
python3 -m http.server 5500 -d tools/fcm-test
```

Windows에서 `python3`가 안 되면(Microsoft Store 별칭) `python`을 씁니다. 브라우저로 **`http://localhost:5500`** 을 엽니다. `127.0.0.1`은 다른 origin이라 main의 `CORS_ALLOWED_ORIGINS`(기본 `http://localhost:5173,http://localhost:5500`)에 걸립니다.

## 단계

페이지 위쪽에 상태가 표시됩니다 (`body`의 `data-fcm-state`: `config-missing`, `config-invalid`, `unsupported`, `sdk-load-failed`, `ready`). `ready`가 되면:

1. 로그인: main 주소(기본 `http://localhost:8080`, `.env`의 `MAIN_HOST_PORT`를 바꿨으면 그 포트)와 전화번호 → `POST /members/login`. JWT는 메모리에만 두고 저장하지 않습니다 (main 주소만 `localStorage`에 기억).
2. 알림 권한 요청.
3. 서비스 워커 `firebase-messaging-sw.js` 등록 + `getToken(VAPID)`로 FCM 토큰 발급.
4. `POST /notification/new`(헤더 `access-token`)로 토큰 저장.
5. `POST /alarm/send-fcm/{memberId}`로 테스트 발송 → 알림 수신. 페이지가 열려 있으면(포그라운드) 알림 대신 화면 로그에 표시됩니다. 다른 탭이나 최소화 상태(백그라운드)에서 브라우저 알림이 뜹니다.

이전 단계가 끝나지 않으면 다음 버튼은 비활성입니다. 요청·응답은 화면 로그에 남습니다 (토큰은 앞 8글자만).

## 문제 해결

- `fetch` 오류(TypeError): main 주소, main 기동 여부, `CORS_ALLOWED_ORIGINS`에 `http://localhost:5500`이 있는지 확인합니다.
- 권한이 거부됨: 주소창의 사이트 설정에서 알림을 허용하고 다시 시도합니다.
- 발송 응답은 200인데 알림이 없음: main 로그(`docker compose logs main`)를 확인합니다. `FCM 비활성: 발송 건너뜀`이면 서버가 `FCM_ENABLED=false`이고, `FCM 발송 완료`면 FCM까지는 전달된 것이라 브라우저 알림 설정을 봅니다. 아무 FCM 로그가 없으면 토큰이 저장되지 않은 것이라 4단계를 다시 합니다.
- 토큰 오류: `FCM 발송 실패` 로그의 `code`를 봅니다. `FCM 토큰이 유효하지 않음(UNREGISTERED)`이 찍히면 서버가 저장된 토큰을 지우므로 3~4단계를 다시 합니다.
- `sdk-load-failed`: 인터넷 연결과 `sdk-version.js`의 버전을 확인합니다.

## 주의

- `firebase-config.js`를 커밋하지 않습니다 (git 제외 상태 유지).
- HTTP 도메인으로 배포하면 서비스 워커·Push API가 동작하지 않아 웹푸시를 쓸 수 없습니다 (`localhost`만 예외, O-7).
- `/alarm/send-fcm`과 전화번호 로그인은 local 프로필 전용입니다.
