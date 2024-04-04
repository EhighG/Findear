importScripts("https://www.gstatic.com/firebasejs/8.10.0/firebase-app.js");
importScripts(
  "https://www.gstatic.com/firebasejs/8.10.0/firebase-messaging.js"
);
// Initialize the Firebase app in the service worker by passing in
// your app's Firebase config object.
// https://firebase.google.com/docs/web/setup#config-object

const firebaseConfig = {
  apiKey: "AIzaSyDME80QnR6HOZ58La5kV5yVSAlpakUotRk",
  authDomain: "findear-bfd63.firebaseapp.com",
  projectId: "findear-bfd63",
  storageBucket: "findear-bfd63.appspot.com",
  messagingSenderId: "257208221402",
  appId: "1:257208221402:web:33f3892aec00a29d756fe3",
  measurementId: "G-KTHK2G867M",
};
firebase.initializeApp(firebaseConfig);

const messaging = firebase.messaging();

messaging.onBackgroundMessage(function (payload) {
  console.log(
    "[firebase-messaging-sw.js] PAYLOAD NOTIFICATION: ",
    payload.notification
  );
  // Customize notification here
  const notificationTitle = payload.notification.title;
  const message = payload.notification.body.split(":") ?? [];
  const notificationOptions = {
    body: message[0],
    icon: payload.notification?.image,
  };

  self.registration.showNotification(notificationTitle, notificationOptions);
});
