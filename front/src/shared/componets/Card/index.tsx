import { Text } from "@/shared";

type CardProps = {
  image: string;
  title: string;
  locate: string;
  date: string;
  onClick?: () => void;
};

const Card = ({ image, title, locate, date, onClick }: CardProps) => {
  return (
    <div className="flex flex-col rounded-2xl w-full" onClick={onClick}>
      <div className="w-full h-auto rounded-t-2xl border-2 border-A706DarkGrey1 dark:border-A706Grey2">
        <img
          src={image}
          alt="이미지없음"
          className="w-full h-full object-contain"
        />
      </div>
      <div className="flex flex-col w-full h-[30%]">
        <Text className="text-[1rem] font-bold"> {title} </Text>
        <Text className="text-[12px]">분실위치 : {locate}</Text>
        <Text className="text-[12px]">분실일자 : {date}</Text>
      </div>
    </div>
  );
};

export default Card;
