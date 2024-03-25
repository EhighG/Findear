import { EventSourcePolyfill } from "event-source-polyfill";
import { useMemberStore } from "@/shared";

const SSEConnect = () => {
  const eventSource = new EventSourcePolyfill(
    import.meta.env.VITE_BASE_URL +
      `subscribe/${useMemberStore.getState().member.memberId}`,
    {
      headers: {
        "access-token": useMemberStore.getState().token.accessToken,
        "Content-Type": "text/event-stream; charset=UTF-8",
      },
    }
  );
  return eventSource;
};

export { SSEConnect };
