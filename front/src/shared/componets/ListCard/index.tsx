import { roomListType } from "@/entities";
import { Text, cls } from "@/shared";
import dayjs from "dayjs";

type ListCardProps = {
  className?: string;
  listData: roomListType;
  onClick?: () => void;
};

const ListCard = ({ className, listData, onClick }: ListCardProps) => {
  return (
    <div
      className={cls(
        className ?? "",
        "flex w-full h-[80px] border-b border-A706Grey2"
      )}
      onClick={onClick}
    >
      <div className="flex w-[20%] ">
        <img
          src={listData?.thumbnailUrl ?? ""}
          alt="thumbnail"
          className="object-fill w-full h-full "
        />
      </div>
      <div className="flex flex-col p-2 w-[80%] ">
        <div className="flex w-full  items-center justify-between">
          <Text className="font-bold text-[1.2rem] truncate">
            {listData?.title}
          </Text>
        </div>
        <div className="flex w-full items-center justify-between">
          <Text className="text-A706SlateGrey truncate">
            {listData?.content}
          </Text>
          <Text className="text-A706SlateGrey text-[0.8rem] text-nowrap">
            {dayjs(listData?.sendAt).format("YY-MM-DD HH:mm")}
          </Text>
        </div>
      </div>
    </div>
  );
};

export default ListCard;
