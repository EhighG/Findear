import { cls } from "@/shared";

type BentoCardProps = {
  className?: string;
  children: JSX.Element | JSX.Element[];
};

const BentoCard = ({ className, children }: BentoCardProps) => {
  return (
    <div
      className={cls(
        className ?? "",
        "p-6 border border-purple-300 rounded-[12px] flex flex-col items-center justify-center gap-1 shadow-lg bg-gradient-to-r from-A706Blue to-white"
      )}
    >
      {children}
    </div>
  );
};

export default BentoCard;
