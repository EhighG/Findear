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

const isSupported = () =>
  "Notification" in window && "serviceWorker" in navigator;

export async function requestUserPermission() {
  if (!isSupported()) {
    return;
  }

  const permission = await Notification.requestPermission();

  if (permission === "granted") {
    getToken(messaging, {
      vapidKey: import.meta.env.VITE_VAPID_PUBLIC_KEY,
    }).then((currentToken) => {
      sendFcmToken(
        currentToken,
        (response) => {
          Swal.fire({
            title: "알림 승인",
            text: "알림 승인이 정상적으로 완료되었습니다",
            icon: "success",
            confirmButtonText: "확인",
          });
          console.log(response);
        },
        () => {
          Swal.fire({
            title: "알림 승인 실패",
            text: "알림 승인에 실패하였습니다",
            icon: "error",
            confirmButtonText: "확인",
          });
        }
      );
    });
  } else {
    Swal.fire({
      title: "알림 권한 요청",
      text: "알림 권한을 허용해주셔야 매칭 알림을 받을 수 있습니다",
      icon: "info",
      confirmButtonText: "확인",
    });
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
    confirmButtonText: message[1] === "message" ? "채팅방으로 이동" : "확인",
    showCancelButton: message[1] === "message" ? true : false,
  }).then((result) => {
    if (result.isConfirmed && message[1] === "message")
      window.location.href = "/letter";
  });
});
