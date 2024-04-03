import { cls } from "@/shared";

type BentoCardProps = {
  className?: string;
  children: JSX.Element | JSX.Element[];
  onClick?: () => void;
};

const BentoCard = ({ className, children, onClick }: BentoCardProps) => {
  return (
    <div
      onClick={onClick}
      className={cls(
        className ?? "",
        "p-6 border border-purple-300 rounded-[12px] flex flex-col items-center justify-center gap-1 shadow-lg bg-gradient-to-r from-A706Blue to-white hover:animate-bounce dark:text-A706Dark"
      )}
    >
      {children}
    </div>
  );
};

export default BentoCard;
