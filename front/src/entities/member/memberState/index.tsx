import type { Member } from "@/entities";

type tokenType = {
  accessToken: string;
  refreshToken: string;
};

interface MemberState {
  member: Member | null;
  token: tokenType;
  setToken: (token: tokenType) => void;
  login: (id: String, password: String) => Member | null;
  logout: () => void;

  isLogin: () => boolean;
}

export default MemberState;
