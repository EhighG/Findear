import { EventSourcePolyfill } from "event-source-polyfill";
import { useMemberStore } from "@/shared";

const SSEConnect = () => {
  const eventSource = new EventSourcePolyfill(
    import.meta.env.VITE_BASE_URL +
      `alarm/subscribe/${useMemberStore.getState().member.memberId}`,
    {
      headers: {
        "access-token": useMemberStore.getState().token.accessToken,
        "Content-Type": "text/event-stream; charset=UTF-8",
      },
      heartbeatTimeout: 3600 * 1000,
    }
  );
  return eventSource;
};

export { SSEConnect };
