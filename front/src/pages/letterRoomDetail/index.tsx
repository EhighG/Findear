import { useEffect } from "react";
import { useParams } from "react-router-dom";
import { getRoomDetail } from "@/entities";
const LetterRoomDetail = () => {
  const roomId = useParams().roomId;
  useEffect(() => {
    if (roomId) {
      getRoomDetail(
        parseInt(roomId),
        ({ data }) => {
          console.log(data);
        },
        (error) => {
          console.log(error);
        }
      );
    }
  }, [roomId]);

  return (
    <div className="flex flex-col flex-1">
      <div className="chat chat-start">
        <div className="chat-header">
          Obi-Wan Kenobi
          <time className="text-xs opacity-50">2 hours ago</time>
        </div>
        <div className="chat-bubble">You were the Chosen One!</div>
        <div className="chat-footer opacity-50">Seen</div>
      </div>
      <div className="chat chat-start">
        <div className="chat-header">
          Obi-Wan Kenobi
          <time className="text-xs opacity-50">2 hour ago</time>
        </div>
        <div className="chat-bubble">I loved you.</div>
        <div className="chat-footer opacity-50">Delivered</div>
      </div>{" "}
    </div>
  );
};

export default LetterRoomDetail;
