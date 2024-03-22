interface Member {
  memberId?: number;
  email?: String;
  role?: String;
  password?: String;
  nickname?: String;
  phoneNumber?: String;
  joinedAt?: Date;
  withdrawalAt?: Date | null;
  withdrawalYn?: Boolean;
}

export default Member;
