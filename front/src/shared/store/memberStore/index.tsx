import type { MemberState } from "@/entities";
import { create } from "zustand";

const useMemberStore = create<MemberState>((set, get) => ({
  member: null,
  login: (/*id: String, Password: String*/) => {
    if (get().isLogin()) {
      get().logout();
      return null;
    } else {
      set({
        member: {
          memberId: 1,
          email: "email@email.com",
          role: "NORMAL",
          password: "password",
          nickname: "닉네임",
          phoneNumber: "010-1234-5678",
          joinedAt: new Date(),
          withdrawalYn: false,
        },
      });
      return true;
    }
  },
  logout: () => set({ member: null }),
  isLogin: () => (get().member ? true : false),
}));

export default useMemberStore;
