import type { MemberState } from "@/entities";
import { create } from "zustand";
import { createJSONStorage, persist } from "zustand/middleware";

export const useMemberStore = create<MemberState>()(
  persist(
    (set, get) => ({
      member: { memberId: -1, phoneNumber: "", role: "NORMAL" },
      token: {
        accessToken: "",
        refreshToken: "",
      },
      Authenticate: false,
      setToken: (token) => set({ token }),
      setMember: (member) => set({ member }),
      setAuthenticate: (auth) => set({ Authenticate: auth }),
      getAuthenticate: () => get().Authenticate,
      getMember: () => get().member,
      getToken: () => get().token,
      // logout: () => set({ member: null }),
      // isLogin: () => (get().member ? true : false),
    }),
    {
      name: "member-Storage",
      storage: createJSONStorage(() => localStorage),
    }
  )
);
