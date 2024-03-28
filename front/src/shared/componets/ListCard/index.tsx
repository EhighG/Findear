import { roomListType } from "@/entities";
import { Text, cls } from "@/shared";
import dayjs from "dayjs";
import { Tooltip } from "flowbite-react";
import { useEffect } from "react";

type ListCardProps = {
  className?: string;
  listData: roomListType;
  onClick?: () => void;
};

const ListCard = ({ className, listData, onClick }: ListCardProps) => {
  useEffect(() => {
    console.log(listData);
  }, []);
  return (
    <div
      className={cls(
        className ?? "",
        "flex w-full h-[80px] border-b border-A706Grey2 p-2"
      )}
      onClick={onClick}
    >
      {/* 클릭하면 상세 채팅방으로 */}
      <div className="flex flex-1">
        <Tooltip content="물건이름">
          <img
            src={listData?.thumbnailUrl ?? ""}
            alt="thumbnail"
            className="inset-0 w-full h-full"
          />
        </Tooltip>
      </div>
      <div className="flex flex-col flex-[4]">
        <div className="flex w-full items-center justify-between">
          <Text className="font-bold text-[1.5rem] truncate">
            {listData?.title}
          </Text>
          <Text className="text-A706SlateGrey">
            {dayjs(listData?.sendAt).format("YY-MM-DD HH:mm")}
          </Text>
        </div>
        <div className="flex w-full items-center justify-between">
          <Text className="text-A706SlateGrey">{listData?.content}</Text>
        </div>
      </div>
      {/* {children} */}
    </div>
  );
};

export default ListCard;
