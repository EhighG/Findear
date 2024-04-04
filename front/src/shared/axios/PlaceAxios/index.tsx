import axios from "axios";

// 장소 axios
const PlaceAxios = () => {
  const instance = axios.create({
    baseURL: import.meta.env.VITE_PLACE_SEARCH_URL,
  });

  // 모든 요청에 대해 기본 헤더 속성 설정
  instance.defaults.headers.common["Content-Type"] =
    "application/json; charset=utf8";

  instance.interceptors.request.use(
    (config) => {
      return config;
    },

    (error) => {
      return Promise.reject(error);
    }
  );

  instance.interceptors.response.use(
    (response) => {
      return response;
    },
    (error) => {
      return Promise.reject(error);
    }
  );

  return instance;
};

export default PlaceAxios;
