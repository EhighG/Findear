import { StateContext } from "@/shared";
import { HeaderLogo, HeaderMenu } from "@/widgets";
import { useContext } from "react";
import { Text } from "@/shared";
import { Link } from "react-router-dom";
const Header = () => {
  const { headerTitle } = useContext(StateContext);
  return (
    <header className="header">
      <Link to="/">
        <HeaderLogo />
      </Link>
      {headerTitle && (
        <div className="absolute w-full text-center">
          <Text className="text-[1.5rem] font-bold">{headerTitle}</Text>
        </div>
      )}
      <HeaderMenu />
    </header>
  );
};

export default Header;
