import { ListCard, StateContext } from "@/shared";
import { useContext, useEffect, useState } from "react";
import { Helmet } from "react-helmet-async";
import { getRoomList } from "@/entities";
import type { roomListType } from "@/entities";
import { useNavigate } from "react-router-dom";
const Letter = () => {
  const navigate = useNavigate();
  const [roomList, setRoomList] = useState<roomListType[]>([]);

  const { setHeaderTitle } = useContext(StateContext);

  useEffect(() => {
    setHeaderTitle("쪽지");
    return () => {
      setHeaderTitle("");
    };
  }, []);

  useEffect(() => {
    getRoomList(
      ({ data }) => {
        setRoomList(data.result);
      },
      (error) => {
        console.log(error);
      }
    );
  }, []);

  return (
    <div className="flex flex-col flex-1">
      <Helmet>
        <title>쪽지 페이지</title>
        <meta name="description" content="파인디어 쪽지 페이지" />
        <meta name="keywords" content="Findear, 파인디어, 쪽지" />
      </Helmet>
      {!roomList && (
        <div className="absolute top-[50%] self-center">
          쪽지가 존재하지 않습니다
        </div>
      )}
      {roomList &&
        roomList?.map((room) => (
          <ListCard
            listData={room}
            key={room.messageRoomId}
            onClick={() => {
              navigate(`/letter/${room.messageRoomId}`);
            }}
          />
        ))}
    </div>
  );
};

export default Letter;
