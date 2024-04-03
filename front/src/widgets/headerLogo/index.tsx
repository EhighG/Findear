import { Text } from "@/shared";

const HeaderLogo = () => {
  return (
    <div className="flex flex-col justify-center w-[80px] h-[80px]">
      <div className="flex flex-col items-center justify-center">
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Magnifying%20Glass%20Tilted%20Right.png"
          alt="Magnifying Glass Tilted Right"
          className="size-[36px]"
        />
        <Text className="text-[12px] font-bold">파인디어</Text>
      </div>
    </div>
  );
};

export default HeaderLogo;
