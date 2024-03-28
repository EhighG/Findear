import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getRoomDetail, roomDetailType, sendMessage } from "@/entities";
import dayjs from "dayjs";
import { CustomButton, Text, cls, useMemberStore } from "@/shared";
import { Label, TextInput, Textarea } from "flowbite-react";
import { AnimatePresence, motion } from "framer-motion";
import { IoCloseSharp } from "react-icons/io5";
const LetterRoomDetail = () => {
  const roomId = useParams().roomId;
  const [detailData, setDetailData] = useState<roomDetailType>();
  const [openChat, setOpenChat] = useState(false);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");

  const { member } = useMemberStore();
  const navigate = useNavigate();
  useEffect(() => {
    if (roomId) {
      getRoomDetail(
        parseInt(roomId),
        ({ data }) => {
          setDetailData(data.result);
          console.log(data);
        },
        (error) => {
          console.log(error);
        }
      );
    }
  }, [roomId]);

  const handleBoard = () => {
    navigate(`/foundItemDetail/${detailData?.board.boardId}`);
  };

  const sendMessageHandler = () => {
    if (!detailData) return;
    sendMessage(
      {
        boardId: detailData.board.boardId,
        title,
        content,
        sender: member.memberId,
      },
      () => {
        window.location.reload();
      },
      (error) => {
        console.log(error);
      }
    );
  };

  return (
    <div className="flex flex-col flex-1 relative">
      <AnimatePresence>
        {openChat && (
          <motion.div
            initial={{ y: 800 }}
            animate={{ y: 0 }}
            exit={{ y: 800 }}
            transition={{ ease: "easeOut", duration: 0.3 }}
            className="absolute inset-x-0 inset-y-0 w-full h-full rounded-lg bg-A706LightGrey z-[10] overflow-x-hidden"
          >
            <div className="flex items-center justify-between px-[10px]">
              <Text className="text-[1.5rem] font-bold p-[10px]">
                쪽지 보내기
              </Text>
              <div
                onClick={() => {
                  setOpenChat(false);
                }}
              >
                <IoCloseSharp size="32" />
              </div>
            </div>
            <div className="p-5">
              <div className="pb-2 block">
                <Label htmlFor="title" color="success" value="쪽지 제목" />
              </div>
              <TextInput
                id="title"
                placeholder="쪽지 제목을 입력해주세요"
                autoComplete="off"
                required
                value={title}
                onChange={(e) => setTitle(e.target.value)}
              />
            </div>
            <div className="p-5">
              <div className="pb-2 block">
                <Label htmlFor="content" value="쪽지 내용" />
              </div>
              <Textarea
                id="content"
                placeholder="쪽지 내용을 적어주세요"
                required
                rows={10}
                value={content}
                onChange={(e) => setContent(e.target.value)}
              />
            </div>
            <div className="flex justify-center p-5">
              <CustomButton
                className="menubtn"
                onClick={() => sendMessageHandler()}
              >
                쪽지 전송
              </CustomButton>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
      <div className="flex w-full h-[80px] border-b border-A706Grey2 p-2">
        <div className="flex flex-1" onClick={() => handleBoard()}>
          <img
            src={detailData?.board.thumbnailUrl ?? ""}
            alt="thumbnail"
            className="inset-0 w-full h-full"
          />
        </div>
        <div className="flex flex-col flex-[4]">
          <div
            className="flex w-full items-center justify-between"
            onClick={() => handleBoard()}
          >
            <Text className="font-bold text-[1.5rem] truncate">
              {detailData ? detailData.board.productName : "물건 제목"}
            </Text>
          </div>
          <div className="flex w-full items-center justify-end">
            <CustomButton
              className="border border-A706Grey2 rounded-md p-1 bg-A706SubBlue"
              onClick={() => {
                setOpenChat(true);
              }}
            >
              <Text className="text-A706LightGrey">쪽지 보내기</Text>
            </CustomButton>
          </div>
        </div>
      </div>
      {detailData?.message.map((message) => (
        <div
          key={message.messageId}
          className={cls(
            "chat",
            message.senderId === member.memberId ? "chat-end" : "chat-start"
          )}
        >
          <div className="chat-header ">
            {message.title}
            <time className="ml-2 text-xs opacity-50">
              {dayjs(message.sendAt).format("YY-MM-DD HH:MM")}
            </time>
          </div>
          <div className="chat-bubble">{message.content}</div>
        </div>
      ))}
    </div>
  );
};

export default LetterRoomDetail;
