import { MainList, MainNavBar } from "@/widgets";
import { SSEConnect, useMemberStore } from "@/shared";
import { useEffect } from "react";
const Main = () => {
  let eventSource: EventSource;
  const SSEConnection = () => {
    console.log("연결 시도");
    try {
      console.log("연결을 시도해봄");

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
    console.log("연결시도");
    console.log(useMemberStore.getState().member);
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
