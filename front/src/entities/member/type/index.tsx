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

type AgencyType = {
  name: string;
  address: string;
  xpos: number;
  ypos: number;
};

type postionType = {
  xPos: number;
  yPos: number;
};

type MemberProps = {
  phoneNumber: string;
  joinedAt: string;
  role: string;
  memberId: number;
  agency: Agency;
};

type dataType = {
  address_name: string;
  category_group_code: string;
  category_group_name: string;
  category_name: string;
  distance: string;
  id: string;
  phone: string;
  place_name: string;
  place_url: string;
  road_address_name: string;
  x: string;
  y: string;
};

type SignupData = {
  phoneNumber: string;
  password: string;
};

type PhoneProps = Pick<SignupData, "phoneNumber">;

type UserData = {
  memberId: number;
  role: string;
  phoneNumber: string;
  agency?: AgencyType;
};

type MemberId = {
  memberId: number;
};

type resetPasswordType = {
  memberId: number;
  oldPassword: string;
  newPassword: string;
};

type patchAgencyType = {
  memberId: number;
  role: string;
  phoneNumber: string;
  agency: AgencyType;
};

type CodeType = {
  email: string;
  code: string;
};

export type {
  Member,
  Agency,
  postionType,
  dataType,
  CodeType,
  MemberId,
  PhoneProps,
  SignupData,
  UserData,
  patchAgencyType,
  resetPasswordType,
  MemberProps,
  AgencyType,
};
