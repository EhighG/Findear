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
        "flex w-full h-[80px] border-b border-A706Grey2"
      )}
      onClick={onClick}
    >
      {/* 클릭하면 상세 채팅방으로 */}
      <div className="flex w-[20%]">
        <Tooltip content="물건이름">
          <img
            src={listData?.thumbnailUrl ?? ""}
            alt="thumbnail"
            className="inset-0 w-full h-full"
          />
        </Tooltip>
      </div>
      <div className="flex flex-col p-2 w-[80%]">
        <div className="flex w-full  items-center justify-between">
          <Text className="font-bold text-[1.2rem] truncate">
            {listData?.title}
          </Text>
        </div>
        <div className="flex w-full items-center justify-between">
          <Text className="text-A706SlateGrey truncate">
            {listData?.content}
          </Text>
          <Text className="text-A706SlateGrey text-[0.8rem] w-[120px]">
            {dayjs(listData?.sendAt).format("YY-MM-DD HH:mm")}
          </Text>
        </div>
      </div>
    </div>
  );
};

export default ListCard;
