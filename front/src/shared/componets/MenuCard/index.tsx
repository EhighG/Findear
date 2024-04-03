import { Text } from "@/shared";

type MenuCardProps = {
  title: string;
  content: string;
  onClick?: () => void;
  generatedAt: string;
};

const MenuCard = ({ title, content, onClick, generatedAt }: MenuCardProps) => {
  return (
    <div
      className="flex h-[90px] rounded-md bg-white dark:bg-A706Blue2 gap-[10px] p-[10px] cursor-pointer"
      onClick={onClick}
    >
      <div className="flex flex-col w-full dark:text-A706DarkGrey1">
        <div className="flex w-full items-center justify-between">
          <Text className="text-[1.2rem] font-bold">{title}</Text>
          <Text className="text-[0.8rem] text-A706Grey">{generatedAt}</Text>
        </div>
        <Text className="text-[1rem]">{content}</Text>
      </div>
    </div>
  );
};

export default MenuCard;
