import ReactDOM from "react-dom/client";
import App from "@/app/App";
import ServiceWorker from "./SW";

ReactDOM.createRoot(document.getElementById("root")!).render(<App />);

ServiceWorker();
