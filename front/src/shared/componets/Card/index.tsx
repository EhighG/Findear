import { Text } from "@/shared";

type CardProps = {
  image: string;
  title: string;
  locate: string;
  date: string;
  type: "분실물" | "습득물";
  onClick?: () => void;
};

const Card = ({ image, title, locate, date, onClick, type }: CardProps) => {
  return (
    <div
      className="flex flex-col rounded-lg border-2 border-A706LightGrey2 dark:border-A706Grey2 w-full shadow-md"
      onClick={onClick}
    >
      <div className="w-full h-auto rounded-t-2xl border-b-2 border-border-A706LightGrey2 dark:border-A706Grey2 ">
        <img
          src={image}
          alt="이미지없음"
          className="w-full h-full object-fill object-center "
        />
      </div>
      <div className="flex flex-col w-full h-[30%] p-1">
        <Text className="text-[1rem] font-bold"> {title} </Text>
        <Text className="text-[12px]">
          {type === "분실물" ? "분실" : "습득"}위치 : {locate}
        </Text>
        <Text className="text-[12px]">
          {type === "분실물" ? "분실" : "습득"}일자 : {date}
        </Text>
      </div>
    </div>
  );
};

export default Card;
