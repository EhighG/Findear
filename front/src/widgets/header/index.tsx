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
        <div>
          <Text className="text-[2rem] font-bold">{headerTitle}</Text>
        </div>
      )}
      <HeaderMenu />
    </header>
  );
};

export default Header;
