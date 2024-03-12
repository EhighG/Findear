import {
  FoundItemButton,
  LostItemButton,
  HomeButton,
  LetterButton,
  InformationButton,
} from "@/widgets";
import { useMemberStore } from "@/shared";

const Footer = () => {
  const { member } = useMemberStore();
  if (!member) {
    return (
      <>
        <footer className="footer">
          <LostItemButton />
          <FoundItemButton />
          <HomeButton />
          <InformationButton />
        </footer>
      </>
    );
  } else {
    return (
      <>
        <footer className="footer">
          <LostItemButton />
          <FoundItemButton />
          <HomeButton />
          <LetterButton />
          <InformationButton />
        </footer>
      </>
    );
  }
};

export default Footer;
