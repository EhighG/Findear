const ServiceWorker = () => {
  const swUrl = `/ServiceWorker.js`;
  navigator.serviceWorker.register(swUrl).then((response) => {
    console.warn("response", response);
  });
};

export default ServiceWorker;
