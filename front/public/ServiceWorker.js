let cacheData = "appV1";

// 서비스 워커 설치
self.addEventListener("install", (event) => {
  console.log("install", event);
});

// 서비스 워커 활성화
self.addEventListener("activate", function () {
  console.log("fcm sw activate..");
});

self.addEventListener("push", (event) => {
  const data = event.data.json();
  console.log("push data", data);

  self.registration.showNotification(data.title, {
    body: data.message,
  });
});
