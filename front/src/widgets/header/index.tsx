import { StateContext, useMemberStore } from "@/shared";
import { HeaderLogo, HeaderMenu } from "@/widgets";
import { useContext } from "react";
import { Text } from "@/shared";
import { Link } from "react-router-dom";
import { Button } from "flowbite-react";
const Header = () => {
  const { member, setMember } = useMemberStore();
  const { headerTitle } = useContext(StateContext);
  return (
    <header className="header">
      <Link to="/">
        <HeaderLogo />
      </Link>
      <Text>{member.role}</Text>
      <Button
        onClick={() => {
          member.role = "NORMAL";
          setMember(member);
        }}
      >
        일반
      </Button>
      <Button
        onClick={() => {
          member.role = "MANAGER";
          setMember(member);
        }}
      >
        관리자
      </Button>
      {headerTitle && (
        <div>
          <Text className="text-[1.5rem] font-bold">{headerTitle}</Text>
        </div>
      )}
      <HeaderMenu />
    </header>
  );
};

export default Header;
