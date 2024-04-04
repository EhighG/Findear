import axios, {
  AxiosRequestConfig,
  AxiosRequestHeaders,
  AxiosResponse,
  AxiosError,
} from "axios";

interface AdaptAxiosRequestConfig extends AxiosRequestConfig {
  headers: AxiosRequestHeaders;
}

const FindearAxios = () => {
  const instance = axios.create({
    baseURL: import.meta.env.VITE_BATCH_URL,
  });

  instance.interceptors.request.use(
    (config: AdaptAxiosRequestConfig) => {
      return config;
    },

    (error: AxiosError) => {
      return Promise.reject(error);
    }
  );

  instance.interceptors.response.use(
    (response: AxiosResponse) => {
      return response;
    },

    (error: AxiosError) => {
      return Promise.reject(error);
    }
  );

  return instance;
};

export default FindearAxios;
