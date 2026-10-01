// FCM 웹푸시 서비스 워커 (Firebase 공식 문서 "Receive messages in JavaScript clients" 의 서비스 워커 방식).
// 번들러 없이 쓰므로 문서가 보여 주는 compat 라이브러리를 importScripts 로 불러온다.
// https://firebase.google.com/docs/cloud-messaging/js/receive
//
// 설정(firebase-config.js)이 없거나 비어 있으면 아무것도 초기화하지 않는다. (페이지도 설정이 없으면 이 워커를 등록하지 않는다.)

// 문서: 알림 클릭 동작을 직접 정의하려면 FCM 라이브러리를 불러오기 "전에" notificationclick 을 등록해야
// FCM 이 덮어쓰지 않는다. 그래서 이 핸들러가 importScripts(FCM) 보다 앞에 있다.
self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((clients) => {
      // 이미 열린 페이지가 있으면 그쪽으로 포커스, 없으면 테스트 페이지를 연다.
      for (const client of clients) {
        if ("focus" in client) return client.focus();
      }
      return self.clients.openWindow("./");
    })
  );
});

let config = null;
try {
  // sdk-version.js: 페이지와 공유하는 SDK 버전의 유일한 정의. firebase-config.js: 사용자가 만든 설정.
  importScripts("sdk-version.js", "firebase-config.js");
  config = self.FINDEAR_FCM_CONFIG || null;
} catch (e) {
  console.warn("[firebase-messaging-sw] 설정 파일을 읽지 못해 FCM 을 초기화하지 않습니다:", e);
}

const firebaseConfig = config && config.firebaseConfig;
const configured =
  firebaseConfig &&
  ["apiKey", "projectId", "messagingSenderId", "appId"].every(
    (key) => typeof firebaseConfig[key] === "string" && firebaseConfig[key].trim() !== ""
  );

if (configured && self.FINDEAR_FIREBASE_SDK_VERSION) {
  const version = self.FINDEAR_FIREBASE_SDK_VERSION;
  importScripts(
    `https://www.gstatic.com/firebasejs/${version}/firebase-app-compat.js`,
    `https://www.gstatic.com/firebasejs/${version}/firebase-messaging-compat.js`
  );

  firebase.initializeApp(firebaseConfig);
  const messaging = firebase.messaging();

  // 문서: notification 페이로드가 있는 메시지는 백그라운드일 때 SDK 가 알림을 자동으로 표시한다.
  // 여기서 showNotification 을 또 부르면 알림이 두 번 뜨므로, notification 이 없는 data 전용 메시지만 직접 표시한다.
  // (main 의 FcmPushSender 는 항상 notification 페이로드를 보낸다.)
  messaging.onBackgroundMessage((payload) => {
    console.log("[firebase-messaging-sw] 백그라운드 메시지", payload);
    if (payload.notification) return;
    const data = payload.data || {};
    self.registration.showNotification(data.title || "Findear", { body: data.body || "" });
  });
} else {
  console.warn("[firebase-messaging-sw] 설정 또는 SDK 버전이 없어 FCM 을 초기화하지 않습니다.");
}
