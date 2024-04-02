let cacheData = "appV1";

// 서비스 워커 설치
self.addEventListener("install", (event) => {
  console.log("install", event);
});

// 서비스 워커 활성화
self.addEventListener("activate", function () {
  console.log("fcm sw activate..");
});
