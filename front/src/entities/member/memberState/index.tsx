import type { Member, Agency } from "@/entities";

type tokenType = {
  accessToken: string;
  refreshToken: string;
};

interface MemberState {
  member: Member;
  agency: Agency;
  token: tokenType;
  Authenticate: boolean;
  setToken: (token: tokenType) => void;
  setMember: (member: Member) => void;
  setAgency: (agency: Agency) => void;
  setAuthenticate: (auth: boolean) => void;
  getAuthenticate: () => boolean;
  getMember: () => Member;
  getToken: () => tokenType;
  getAgency: () => Agency;
  tokenInitialize: () => void;
  memberInitialize: () => void;
  authenticateInitialize: () => void;
  agencyInitialize: () => void;
  // login: (id: String, password: String) => Member | null;
  // logout: () => void;

  // isLogin: () => boolean;
}

export default MemberState;
