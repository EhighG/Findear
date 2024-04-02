import { initializeApp } from "firebase/app";
import { getMessaging, getToken, onMessage } from "firebase/messaging";
import { sendFcmToken } from "./entities";
import Swal from "sweetalert2";
// Your web app's Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyDME80QnR6HOZ58La5kV5yVSAlpakUotRk",
  authDomain: "findear-bfd63.firebaseapp.com",
  projectId: "findear-bfd63",
  storageBucket: "findear-bfd63.appspot.com",
  messagingSenderId: "257208221402",
  appId: "1:257208221402:web:33f3892aec00a29d756fe3",
  measurementId: "G-KTHK2G867M",
};
export const app = initializeApp(firebaseConfig);
export const messaging = getMessaging(app);

export async function requestPermission() {
  if (!("Notification" in window)) {
    Swal.fire({
      title: "브라우저 알림 미지원",
      text: "현재 브라우저는 알림을 지원하지 않습니다, 크롬 브라우저를 사용해주세요.",
      icon: "warning",
      confirmButtonText: "확인",
    });
    return;
  }
  const permission = await Notification.requestPermission();

  if (permission === "granted") {
    getToken(messaging, {
      vapidKey: import.meta.env.VITE_VAPID_PUBLIC_KEY,
    })
      .then((currentToken) => {
        sendFcmToken(
          currentToken,
          ({ data }) => {
            console.info(data);
          },
          () => {}
        );
      })
      .catch((err) => {
        console.log("An error occurred while retrieving token. ", err);
      });
  } else if (permission === "denied") {
    console.log("푸시 권한 차단");
  }
}

//포그라운드 메시지 수신
onMessage(messaging, (payload) => {
  // Customize notification here
  // var notificationTitle = payload.notification?.title ?? "Push Notification";
  console.log(payload);
  const message: string[] = payload.notification?.body?.split(":") ?? [];
  Swal.fire({
    title: payload.notification?.title ?? "푸시 알림",
    text: message[0] ?? "메시지",
    icon: "info",
    confirmButtonText: "확인",
  });
});
