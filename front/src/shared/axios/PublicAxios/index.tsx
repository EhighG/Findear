import axios from "axios";

//  공공데이터 Axios
const PublicAxios = () => {
  const instance = axios.create({
    baseURL: import.meta.env.VITE_PUBLIC_API,
  });

  // 모든 요청에 대해 기본 헤더 속성 설정
  instance.defaults.headers.common["Content-Type"] =
    "application/json; charset=utf8";
  instance.defaults.headers.common["Token-Type"] = "bearer";

  instance.defaults.headers.common["Access-Token"] = "";
  instance.defaults.headers.common["Refresh-Token"] = "";

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

export default PublicAxios;
