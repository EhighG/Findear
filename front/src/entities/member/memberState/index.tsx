import type { Member } from "@/entities";

interface MemberState {
  member: Member | null;

  login: (id: String, password: String) => Member | null;
  logout: () => void;

  isLogin: () => boolean;
}

export default MemberState;
