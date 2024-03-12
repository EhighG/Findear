const ServiceWorker = () => {
  let swUrl = `/ServiceWorker.ts`;
  navigator.serviceWorker.register(swUrl).then((response) => {
    console.warn("response", response);
  });
};

export default ServiceWorker;
