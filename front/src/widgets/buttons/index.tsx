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
import { CustomButton } from "@/shared";
const LostItemButton = () => {
  return (
    <CustomButton className="footer-button">
      <>
        <ErrorOutlineIcon className="button-icon" fontSize="large" />
        분실물
      </>
    </CustomButton>
  );
};

const FoundItemButton = () => {
  return (
    <CustomButton type="button" className="footer-button">
      <>
        <InventoryIcon className="button-icon" fontSize="large" />
        습득물
      </>
    </CustomButton>
  );
};

const HomeButton = () => {
  return (
    <CustomButton type="button" className="footer-button">
      <>
        <HomeIcon className="button-icon" fontSize="large" />홈
      </>
    </CustomButton>
  );
};

const LetterButton = () => {
  return (
    <CustomButton type="button" className="footer-button">
      <>
        <EmailIcon className="button-icon" fontSize="large" />
        쪽지
      </>
    </CustomButton>
  );
};

const InformationButton = () => {
  return (
    <CustomButton type="button" className="footer-button">
      <>
        <ContactSupportIcon className="button-icon" fontSize="large" />
        안내
      </>
    </CustomButton>
  );
};

const LoginButton = () => {
  return (
    <CustomButton className="flex flex-col justify-center mr-[10px]">
      <LockIcon fontSize="large" />
    </CustomButton>
  );
};

const SigninButton = () => {
  return (
    <CustomButton className="flex flex-col justify-center mr-[15px]">
      <PersonAddIcon fontSize="large" />
    </CustomButton>
  );
};

const AlramButton = () => {
  return (
    <CustomButton className="flex flex-col justify-center mr-[10px]">
      <NotificationsIcon fontSize="large" />
    </CustomButton>
  );
};

const MyPageButton = () => {
  return (
    <CustomButton className="flex flex-col justify-center mr-[15px]">
      <PersonIcon fontSize="large" />
    </CustomButton>
  );
};

const SearchFoundItemButton = () => {
  return (
    <CustomButton className="main-nav-button">
      <>
        <SearchIcon fontSize="large" />
        습득물 찾기
      </>
    </CustomButton>
  );
};

const RegistFoundItemButton = () => {
  return (
    <CustomButton className="main-nav-button">
      <>
        <InventoryIcon fontSize="large" />
        습득물 등록
      </>
    </CustomButton>
  );
};

const SearchLostItemButton = () => {
  return (
    <CustomButton className="main-nav-button">
      <>
        <ErrorOutlineIcon fontSize="large" />
        분실물 찾기
      </>
    </CustomButton>
  );
};

const RegistLostItemButton = () => {
  return (
    <CustomButton className="main-nav-button">
      <>
        <UnarchiveIcon fontSize="large" />
        분실물 등록
      </>
    </CustomButton>
  );
};

const ScrapListButton = () => {
  return (
    <CustomButton className="main-nav-button">
      <>
        <FavoriteIcon fontSize="large" />
        스크랩 목록
      </>
    </CustomButton>
  );
};

const NavbarMyPageButton = () => {
  return (
    <CustomButton className="main-nav-button">
      <>
        <PersonIcon fontSize="large" />
        마이페이지
      </>
    </CustomButton>
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
