/// <reference types="vite/client" />
interface ImportMetaEnv {
  readonly VITE_BASE_URL: string;
  readonly VITE_PUBLIC_BASE_URL: string;
  readonly VITE_KAKAO_API_KEY: string;
  readonly VITE_PUBLIC_API_KEY: string;
  readonly VITE_S3_ACCESS_KEY: string;
  readonly VITE_S3_SECRET_ACCESS_KEY: string;
  readonly VITE_BATCH_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
