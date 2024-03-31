/// <reference types="vite/client" />
interface ImportMetaEnv {
  readonly VITE_BASE_URL: string;
  readonly VITE_PUBLIC_BASE_URL: string;
  readonly VITE_KAKAO_API_KEY: string;
  readonly VITE_PUBLIC_API_KEY: string;
  readonly VITE_S3_ACCESS_KEY: string;
  readonly VITE_S3_SECRET_ACCESS_KEY: string;
  readonly VITE_BATCH_URL: string;
  readonly VITE_CLIENT_ID: string;
  readonly VITE_CLIENT_SECRET: string;
  readonly VITE_REDIRECT_URI: string;
  readonly VITE_NAVER_LOGIN: string;
  readonly VITE_COMMERCIAL_URL: string;
  readonly VITE_COMMERCIAL_KEY: string;
  readonly VITE_APP_FCM_API_KEY: string;
  readonly VITE_APP_FCM_AUTH_DOMAIN: string;
  readonly VITE_APP_FCM_PROJECT_ID: string;
  readonly VITE_APP_FCM_STORAGE_BUCKET: string;
  readonly VITE_APP_FCM_MESSAGING_SENDER_ID: string;
  readonly VITE_APP_FCM_APP_ID: string;
  readonly VITE_APP_FCM_MEASUREMENT_ID: string;
  readonly VITE_VAPID_PUBLIC_KEY: string;
}
interface ImportMeta {
  readonly env: ImportMetaEnv;
}
