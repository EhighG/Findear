// FCM 테스트 페이지 설정 예시. 이 파일을 firebase-config.js 로 복사해서 값을 채운다.
//   cp tools/fcm-test/firebase-config.example.js tools/fcm-test/firebase-config.js
//
// 값을 얻는 곳 (Firebase 콘솔, 새 프로젝트):
//   firebaseConfig : 프로젝트 설정 → 일반 → 내 앱 → 웹 앱 → SDK 설정 및 구성의 firebaseConfig 객체
//   vapidKey       : 프로젝트 설정 → 클라우드 메시징 → 웹 푸시 인증서 → 키 쌍 생성 후 나오는 "공개키"
//
// firebase-config.js 는 git 에서 제외된다 (루트 .gitignore). 커밋하지 않는다.
//
// self 를 쓰는 이유: 같은 파일을 페이지(window)와 서비스 워커(firebase-messaging-sw.js)가 모두 읽기 때문이다.
self.FINDEAR_FCM_CONFIG = {
  firebaseConfig: {
    apiKey: "",
    authDomain: "",
    projectId: "",
    storageBucket: "",
    messagingSenderId: "",
    appId: "",
  },
  vapidKey: "",
};
