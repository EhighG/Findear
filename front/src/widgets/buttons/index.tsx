import InventoryIcon from "@mui/icons-material/Inventory";
import HomeIcon from "@mui/icons-material/Home";
import ContactSupportIcon from "@mui/icons-material/ContactSupport";
import EmailIcon from "@mui/icons-material/Email";
import ErrorOutlineIcon from "@mui/icons-material/ErrorOutline";
import LockIcon from "@mui/icons-material/Lock";
import PersonAddIcon from "@mui/icons-material/PersonAdd";
import NotificationsIcon from "@mui/icons-material/Notifications";
import PersonIcon from "@mui/icons-material/Person";
import SearchIcon from "@mui/icons-material/Search";
import FavoriteIcon from "@mui/icons-material/Favorite";
import UnarchiveIcon from "@mui/icons-material/Unarchive";

import { useMemberStore } from "@/shared";

const LostItemButton = () => {
  return (
    <>
      <button type="button" className="footer-button">
        <ErrorOutlineIcon className="button-icon" fontSize="large" />
        분실물
      </button>
    </>
  );
};

const FoundItemButton = () => {
  return (
    <>
      <button type="button" className="footer-button">
        <InventoryIcon className="button-icon" fontSize="large" />
        습득물
      </button>
    </>
  );
};

const HomeButton = () => {
  return (
    <>
      <button type="button" className="footer-button">
        <HomeIcon className="button-icon" fontSize="large" />홈
      </button>
    </>
  );
};

const LetterButton = () => {
  return (
    <>
      <button type="button" className="footer-button">
        <EmailIcon className="button-icon" fontSize="large" />
        쪽지
      </button>
    </>
  );
};

const InformationButton = () => {
  return (
    <>
      <button type="button" className="footer-button">
        <ContactSupportIcon className="button-icon" fontSize="large" />
        안내
      </button>
    </>
  );
};

const LoginButton = () => {
  const { login } = useMemberStore();
  return (
    <>
      <button
        className="flex flex-col justify-center mr-[10px]"
        onClick={() => login("id", "password")}
      >
        <LockIcon fontSize="large" />
      </button>
    </>
  );
};

const SigninButton = () => {
  return (
    <>
      <button className="flex flex-col justify-center mr-[15px]">
        <PersonAddIcon fontSize="large" />
      </button>
    </>
  );
};

const AlramButton = () => {
  return (
    <>
      <button className="flex flex-col justify-center mr-[10px]">
        <NotificationsIcon fontSize="large" />
      </button>
    </>
  );
};

const MyPageButton = () => {
  return (
    <>
      <button className="flex flex-col justify-center mr-[15px]">
        <PersonIcon fontSize="large" />
      </button>
    </>
  );
};

const SearchFoundItemButton = () => {
  return (
    <>
      <button className="main-nav-button">
        <SearchIcon fontSize="large" />
        습득물 찾기
      </button>
    </>
  );
};

const RegistFoundItemButton = () => {
  return (
    <>
      <button className="main-nav-button">
        <InventoryIcon fontSize="large" />
        습득물 등록
      </button>
    </>
  );
};

const SearchLostItemButton = () => {
  return (
    <>
      <button className="main-nav-button">
        <ErrorOutlineIcon fontSize="large" />
        분실물 찾기
      </button>
    </>
  );
};

const RegistLostItemButton = () => {
  return (
    <>
      <button className="main-nav-button">
        <UnarchiveIcon fontSize="large" />
        분실물 등록
      </button>
    </>
  );
};

const ScrapListButton = () => {
  return (
    <>
      <button className="main-nav-button">
        <FavoriteIcon fontSize="large" />
        스크랩 목록
      </button>
    </>
  );
};

const NavbarMyPageButton = () => {
  return (
    <>
      <button className="main-nav-button">
        <PersonIcon fontSize="large" />
        마이페이지
      </button>
    </>
  );
};

export {
  FoundItemButton,
  HomeButton,
  InformationButton,
  LetterButton,
  LostItemButton,
  LoginButton,
  SigninButton,
  AlramButton,
  MyPageButton,
  SearchFoundItemButton,
  RegistFoundItemButton,
  SearchLostItemButton,
  RegistLostItemButton,
  ScrapListButton,
  NavbarMyPageButton,
};
