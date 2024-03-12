import {
  NavbarMyPageButton,
  RegistFoundItemButton,
  RegistLostItemButton,
  ScrapListButton,
  SearchFoundItemButton,
  SearchLostItemButton,
} from "@/widgets";
import { useMemberStore } from "@/shared";

const MainNavBar = () => {
  const { member } = useMemberStore();
  if (!member) {
    return (
      <>
        <nav className="main-nav">
          <SearchFoundItemButton />
          <SearchLostItemButton />
        </nav>
      </>
    );
  } else {
    if (member.role === "NORMAL") {
      return (
        <>
          <nav className="main-nav">
            <SearchFoundItemButton />
            <RegistLostItemButton />
            <ScrapListButton />
            <NavbarMyPageButton />
          </nav>
        </>
      );
    } else if (member.role === "MANAGER") {
      return (
        <>
          <nav className="main-nav">
            <SearchFoundItemButton />
            <RegistFoundItemButton />
            <SearchLostItemButton />
            <RegistLostItemButton />
          </nav>
        </>
      );
    }
  }
};

export default MainNavBar;
