import { MainList, MainNavBar } from "@/widgets";
import { SSEConnect } from "@/shared";
import { useEffect } from "react";
const Main = () => {
  let eventSource: EventSource;

  const SSEConnection = () => {
    try {
      eventSource = SSEConnect();
    } catch (error) {
      console.log(error);
    }

    eventSource.onopen = () => {
      console.log("Server Sent Event 연결이 열렸습니다.");
    };

    eventSource.onerror = () => {
      console.log("Server Sent Event 오류");
    };

    eventSource.addEventListener("message", (event) => {
      const data = JSON.parse(event.data);
      console.log(data);
    });
  };

  useEffect(() => {
    SSEConnection();
  }, []);

  return (
    <div className="flex flex-col">
      <MainNavBar />
      <MainList />
    </div>
  );
};

export default Main;
