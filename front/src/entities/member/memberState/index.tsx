import type { Member } from "@/entities";

type tokenType = {
  accessToken: string;
  refreshToken: string;
};

interface MemberState {
  member: Member;
  token: tokenType;
  Authenticate: boolean;
  setToken: (token: tokenType) => void;
  setMember: (member: Member) => void;
  setAuthenticate: (auth: boolean) => void;
  getAuthenticate: () => boolean;
  getMember: () => Member;
  getToken: () => tokenType;
  tokenInitialize: () => void;
  memberInitialize: () => void;
  authenticateInitialize: () => void;
  // login: (id: String, password: String) => Member | null;
  // logout: () => void;

  // isLogin: () => boolean;
}

export default MemberState;
