/// <reference types="vite/client" />
interface ImportMetaEnv {
  readonly VITE_BASE_URL: string;
  readonly VITE_PUBLIC_BASE_URL: string;
  readonly VITE_KAKAO_API_KEY: string;
  readonly VITE_PUBLIC_API_KEY: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
