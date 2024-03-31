import "./Firebase";

const ServiceWorker = async () => {
  if ("serviceWorker" in navigator && "PushManager" in window) {
    const swUrl = `/ServiceWorker.js`;
    navigator.serviceWorker
      .register(swUrl)
      .then((response) => {
        console.log("Service Worker Registered", response);
      })
      .catch((error) => {
        console.error("Service Worker Register Error", error);
      });
  } else {
    console.error("Service Worker is not supported in this browser");
  }
};

export default ServiceWorker;
