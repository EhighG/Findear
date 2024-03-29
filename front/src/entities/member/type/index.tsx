interface Member {
  memberId: number;
  phoneNumber: string;
  role: string;
}

interface Agency {
  id: number;
  name: string;
  address: string;
}

export type { Member, Agency };
