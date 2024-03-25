import {
  FoundItemButton,
  LostItemButton,
  HomeButton,
  LetterButton,
  InformationButton,
} from "@/widgets";
import { useMemberStore } from "@/shared";
import { Link } from "react-router-dom";

const Footer = () => {
  const { Authenticate } = useMemberStore();

  return (
    <footer className="footer">
      <Link to="/losts">
        <LostItemButton />
      </Link>
      <Link to="/acquire">
        <FoundItemButton />
      </Link>
      <Link to="/">
        <HomeButton />
      </Link>
      {Authenticate && (
        <Link to="/letter">
          <LetterButton />
        </Link>
      )}
      <Link to="/introduce">
        <InformationButton />
      </Link>
    </footer>
  );
};

export default Footer;
