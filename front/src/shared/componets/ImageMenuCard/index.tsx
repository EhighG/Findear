import { Text, cls } from "@/shared";
import { useState } from "react";

type ImageMenuCardProps = {
  onClick: () => void;
  title: string;
  image: string;
  alt: string;
  render: number; // 랜더링 순서
  className?: string;
};

const ImageMenuCard = ({
  title,
  image,
  alt,
  onClick,
  render,
  className,
}: ImageMenuCardProps) => {
  const [hidden, setHidden] = useState(true);
  // 순차 랜더링을 위해 render 순서에 따라 시간을 다르게 설정, 시간이 만료되면 hidden을 false로 변경
  setTimeout(() => {
    setHidden(false);
  }, render * 100);
  return (
    <div
      className={cls(
        className ? className : "",
        "flex h-[90px] rounded-lg shadow-sm bg-white dark:bg-A706Blue2 gap-[10px] p-[10px] cursor-pointer animate-fade",
        hidden ? "hidden" : ""
      )}
      onClick={onClick}
    >
      <div className="flex flex-col w-full justify-center">
        <div className="flex justify-between items-center px-[20px]">
          <Text className="text-[1.5rem] font-bold dark:text-A706DarkGrey1">
            {title}
          </Text>
          <img src={image} alt={alt} width="50" height="50" />
        </div>
      </div>
    </div>
  );
};

export default ImageMenuCard;
