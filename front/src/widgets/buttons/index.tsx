import InventoryIcon from "@mui/icons-material/Inventory";
import ErrorOutlineIcon from "@mui/icons-material/ErrorOutline";
import PersonIcon from "@mui/icons-material/Person";
import SearchIcon from "@mui/icons-material/Search";
import FavoriteIcon from "@mui/icons-material/Favorite";
import UnarchiveIcon from "@mui/icons-material/Unarchive";
import { CustomButton, Text } from "@/shared";
const LostItemButton = () => {
  return (
    <CustomButton className="footer-button">
      <>
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Travel%20and%20places/Luggage.png"
          alt="Luggage"
          width="40"
          height="40"
        />
        <Text className="text-[1rem] font-bold text-nowrap">분실물</Text>
      </>
    </CustomButton>
  );
};

const FoundItemButton = () => {
  return (
    <CustomButton type="button" className="footer-button">
      <>
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Inbox%20Tray.png"
          alt="Inbox Tray"
          width="40"
          height="40"
        />
        <Text className="text-[1rem] font-bold text-nowrap">습득물</Text>
      </>
    </CustomButton>
  );
};

const HomeButton = () => {
  return (
    <CustomButton type="button" className="footer-button">
      <>
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Travel%20and%20places/House.png"
          alt="House"
          width="40"
          height="40"
        />
        <Text className="text-[1rem] font-bold">홈</Text>
      </>
    </CustomButton>
  );
};

const LetterButton = () => {
  return (
    <CustomButton type="button" className="footer-button">
      <>
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Envelope%20with%20Arrow.png"
          alt="Envelope with Arrow"
          width="40"
          height="40"
        />
        <Text className="text-[1rem] font-bold">쪽지</Text>
      </>
    </CustomButton>
  );
};

const InformationButton = () => {
  return (
    <CustomButton type="button" className="footer-button">
      <>
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Placard.png"
          alt="Placard"
          width="40"
          height="40"
        />
        <Text className="text-[1rem] font-bold">안내</Text>
      </>
    </CustomButton>
  );
};

const LoginButton = () => {
  return (
    <CustomButton className="flex flex-col justify-center items-center mr-[10px]">
      <>
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Animals/Baby%20Chick.png"
          alt="register"
          width="40"
          height="40"
        />
        <Text className="text-[1rem] font-bold text-nowrap">회원가입</Text>
      </>
    </CustomButton>
  );
};

const SigninButton = () => {
  return (
    <CustomButton className="flex flex-col justify-center items-center mr-[10px]">
      <>
        <img
          src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Placard.png"
          alt="Placard"
          width="40"
          height="40"
        />
        <Text className="text-[1rem] font-bold">가입</Text>
      </>
    </CustomButton>
  );
};

// const AlramButton = () => {
//   return (
//     <CustomButton className="flex flex-col items-center justify-center mr-[10px]">
//       <>
//         <img
//           src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Bell.png"
//           alt="Bell"
//           className="size-[36px]"
//         />
//         <Text className="text-[12px] font-bold">알림</Text>
//       </>
//     </CustomButton>
//   );
// };

// const MyPageButton = () => {
//   return (
//     <CustomButton className="flex flex-col items-center justify-center mr-[10px]">
//       <>
//         <img
//           src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Necktie.png"
//           alt="Necktie"
//           className="size-[36px]"
//         />
//         <Text className="text-[12px] font-bold">마이페이지</Text>
//       </>
//     </CustomButton>
//   );
// };

const AlramButton = () => {
  return (
    <CustomButton className="flex flex-col items-center justify-center mr-[10px]">
      <img
        src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Bell.png"
        alt="Bell"
        width={40}
        height={40}
      />
      <Text className="text-[12px] font-bold text-nowrap">알림</Text>
    </CustomButton>
  );
};

const MyPageButton = () => {
  return (
    <CustomButton className="flex flex-col items-center justify-center mr-[10px]">
      <img
        src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Objects/Necktie.png"
        alt="Necktie"
        width={40}
        height={40}
      />
      <Text className="text-[12px] font-bold text-nowrap">마이페이지</Text>
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
