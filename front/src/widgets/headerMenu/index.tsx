import {
  LoginButton,
  SigninButton,
  AlramButton,
  MyPageButton,
} from "@/widgets";
import { useMemberStore } from "@/shared";
import { Link } from "react-router-dom";

const HeaderMenu = () => {
  const { Authenticate } = useMemberStore();

  if (!Authenticate) {
    return (
      <div className="h-[80px] flex justify-end items-center">
        <Link to="/signin">
          <LoginButton />
        </Link>
        <Link to="/signup">
          <SigninButton />
        </Link>
      </div>
    );
  } else {
    return (
      <div className="h-[80px] flex justify-end items-center">
        <Link to="/alarm">
          <AlramButton />
        </Link>
        <Link to="/myPage">
          <MyPageButton />
        </Link>
      </div>
    );
  }
};

export default HeaderMenu;
