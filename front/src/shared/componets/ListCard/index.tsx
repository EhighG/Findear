import { cls } from "@/shared";

type ListCardProps = {
  className?: string;
  children: React.ReactNode;
};

const ListCard = ({ className, children }: ListCardProps) => {
  return (
    <div
      className={cls(
        className ?? "",
        "flex w-full h-[80px] border-b border-A706Grey2 p-2"
      )}
    >
      {children}
    </div>
  );
};

export default ListCard;
