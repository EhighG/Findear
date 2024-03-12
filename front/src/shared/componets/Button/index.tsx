type buttonProps = {
  className?: string;
  onClick?: () => void;
  children: string;
  type?: "button" | "submit" | "reset"; // 버튼 타입을 명시합니다.
};

const Button = ({
  children,
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
      {children}
    </button>
  );
};

export default Button;
