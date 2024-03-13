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
        <nav className="main-nav mt-[16px] flex justify-center">
          <SearchFoundItemButton />
          <SearchLostItemButton />
        </nav>
      </>
    );
  } else {
    if (member.role === "NORMAL") {
      return (
        <>
          <nav className="main-nav mt-[16px] justify-center">
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
          <nav className="main-nav mt-[16px] justify-center">
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
