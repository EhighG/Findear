import { SyntheticEvent } from "react";

type buttonProps = {
  className?: string;
  onClick?: (event?: SyntheticEvent) => void;
  children: string | JSX.Element;
  disabled?: boolean;
  type?: "button" | "submit" | "reset"; // 버튼 타입을 명시합니다.
};

const Button = ({
  children,
  className,
  disabled,
  onClick,
  type = "button",
}: buttonProps) => {
  return (
    <button
      type={type}
      onClick={onClick}
      className={className ? className : "" + " text-[32px]"}
      disabled={disabled}
    >
      {children}
    </button>
  );
};

export default Button;
