import ReactDOM from "react-dom/client";
import App from "@/app/App";
import ServiceWorker from "./SW";

declare global {
  interface Window {
    kakao: any;
    daum: any;
    AWS: any;
    naver: any;
  }
  const kakao: any;
  const daum: any;
}

ReactDOM.createRoot(document.getElementById("root")!).render(<App />);

ServiceWorker();
