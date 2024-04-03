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
      agency: {
        name: "",
        address: "",
        id: -1,
      },
      Authenticate: false,
      setToken: (token) => set({ token }),
      setMember: (member) => set({ member }),
      setAgency: (agency) => set({ agency }),
      setAuthenticate: (auth) => set({ Authenticate: auth }),
      tokenInitialize: () =>
        set({ token: { accessToken: "", refreshToken: "" } }),
      memberInitialize: () =>
        set({ member: { memberId: -1, phoneNumber: "", role: "NORMAL" } }),
      authenticateInitialize: () => set({ Authenticate: false }),
      getAuthenticate: () => get().Authenticate,
      getMember: () => get().member,
      getToken: () => get().token,
      getAgency: () => get().agency,
      agencyInitialize: () =>
        set({ agency: { name: "", address: "", id: -1 } }),
      // logout: () => set({ member: null }),
      // isLogin: () => (get().member ? true : false),
    }),
    {
      name: "member-Storage",
      storage: createJSONStorage(() => localStorage),
    }
  )
);
