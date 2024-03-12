import {
  LoginButton,
  SigninButton,
  AlramButton,
  MyPageButton,
} from "@/widgets";
import { useMemberStore } from "@/shared";

const HeaderMenu = () => {
  const { member } = useMemberStore();

  if (!member) {
    return (
      <div className="w-[280px] h-[80px] flex justify-end">
        <LoginButton />
        <SigninButton />
      </div>
    );
  } else {
    return (
      <div className="w-[280px] h-[80px] flex justify-end">
        <AlramButton />
        <MyPageButton />
      </div>
    );
  }
};

export default HeaderMenu;
