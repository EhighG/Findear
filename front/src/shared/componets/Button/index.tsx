import { SyntheticEvent } from "react";

type buttonProps = {
  className?: string;
  onClick?: (event?: SyntheticEvent) => void;
  childrens: (string | JSX.Element)[];
  type?: "button" | "submit" | "reset"; // 버튼 타입을 명시합니다.
};

const Button = ({
  childrens,
  className,
  onClick,
  type = "button",
}: buttonProps) => {
  return (
    <button
      type={type}
      onClick={onClick}
      className={className ? className : "" + " text-[32px]"}
    >
      {childrens.map((child) => child)}
    </button>
  );
};

export default Button;
