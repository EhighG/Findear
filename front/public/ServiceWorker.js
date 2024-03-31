let cacheData = "appV1";

// 서비스 워커 설치
self.addEventListener("install", (event) => {
  console.log("install", event);
});

// 서비스 워커 활성화
self.addEventListener("activate", function () {
  console.log("fcm sw activate..");
});

self.addEventListener("push", function (e) {
  const resultData = e.data.json().notification;
  const notificationTitle = resultData.title;
  const notificationOptions = {
    body: resultData.body,
    icon: resultData.image, // 웹 푸시 이미지는 icon
    tag: resultData.tag,
  };

  self.registration.showNotification(notificationTitle, notificationOptions);
});
