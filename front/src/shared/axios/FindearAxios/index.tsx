import axios, {
  AxiosRequestConfig,
  AxiosRequestHeaders,
  AxiosResponse,
  AxiosError,
} from "axios";
import { useMemberStore, httpStatusCode } from "@/shared";

interface AdaptAxiosRequestConfig extends AxiosRequestConfig {
  headers: AxiosRequestHeaders;
}

const FindearAxios = () => {
  const instance = axios.create({
    baseURL: import.meta.env.VITE_BASE_URL,
  });

  // 모든 요청에 대해 기본 헤더 속성 설정
  instance.defaults.headers.common["Content-Type"] =
    "application/json; charset=utf8";
  instance.defaults.headers.common["Token-Type"] = "bearer";
  instance.defaults.headers.common["access-token"] = "";
  instance.defaults.headers.common["refresh-token"] = "";

  instance.interceptors.request.use(
    (config: AdaptAxiosRequestConfig) => {
      config.headers["access-token"] =
        useMemberStore.getState().token.accessToken;
      config.headers["refresh-token"] =
        useMemberStore.getState().token.refreshToken;
      console.log(config);
      return config;
    },

    (error: AxiosError) => {
      return Promise.reject(error);
    }
  );

  // 토큰의 리프레쉬 여부를 감지하는 변수
  let isRefreshing = false;

  instance.interceptors.response.use(
    (response: AxiosResponse) => {
      return response;
    },

    async (error: any) => {
      const {
        config,
        response: { status },
      } = error;

      if (status == httpStatusCode.UNAUTHORIZED) {
        const originalRequest = config;
        if (!isRefreshing) {
          isRefreshing = true;
          console.log("토큰 갱신 요청");

          await instance
            .post("/members/token/refresh")
            .then(({ data }: AxiosResponse) => {
              console.log("토큰 갱신 성공");
              useMemberStore.getState().setToken({
                accessToken: data.accessToken,
                refreshToken: data.refreshToken,
              });
            })
            .catch(() => {
              console.log("토큰 갱신 실패");
              useMemberStore.getState().tokenInitialize();
              useMemberStore.getState().setAuthenticate(false);
              useMemberStore.getState().memberInitialize();
              window.location.href = "/";
              return Promise.reject(error);
            });

          originalRequest.headers["AccessToken"] =
            "bearer " + useMemberStore.getState().token.accessToken;
          originalRequest.headers["RefreshToken"] =
            "bearer " + useMemberStore.getState().token.refreshToken;
          console.log("토큰 갱신후 기존 api요청 재 전송");
          isRefreshing = false;

          return instance(originalRequest);
        }
      } else if (status == httpStatusCode.FORBIDDEN) {
        alert("접근 권한이 없습니다.");
      }

      return Promise.reject(error);
    }
  );

  return instance;
};

export default FindearAxios;
