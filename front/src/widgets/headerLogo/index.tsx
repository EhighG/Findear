import { Findear } from "@/shared";

const HeaderLogo = () => {
  return (
    <div className="flex flex-col justify-center w-[80px] h-[80px]">
      <div className="flex flex-row justify-center">
        <img src={Findear} className="w-[36px] h-[36px]" alt="logo" />
      </div>
    </div>
  );
};

export default HeaderLogo;
