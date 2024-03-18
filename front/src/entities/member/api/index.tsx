import { FindearAxios } from "@/shared";
import { AxiosResponse } from "axios";

type SigninData = {
  email: string;
  password: string;
};

type UserData = {
  member: Member;
  Agency?: Agency;
};

type EmailProps = Pick<SigninData, "email">;

type MemberId = {
  memberId: number;
};

// 회원 정보
type Member = {
  password: string;
  role: "NORMAL" | "MANAGER";
  nickname: string;
  email: string;
  phoneNumber: string;
};

// 대리점 정보
type Agency = {
  name: string;
  phoneNumber: string;
  address: string;
  detailAddress: string;
  xPos: number;
  yPos: number;
};

type resetPasswordType = {
  memberId: number;
  oldPassword: string;
  newPassword: string;
};

const axios = FindearAxios();

const signIn = async (
  data: SigninData,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.post("/members/login", data).then(success).catch(fail);
};

const signUp = async (
  data: UserData,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.post("/members", data).then(success).catch(fail);
};

const signOut = async (
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.post("/members/logout").then(success).catch(fail);
};

const checkEmail = async (
  data: EmailProps,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.post("/members/emails/duplicate", data).then(success).catch(fail);
};

const sendCode = async (
  data: EmailProps,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios
    .post("/members/emails/verify-code", data)
    .then(success)
    .catch(fail);
};

type CodeType = {
  email: string;
  code: string;
};

const checkCode = async (
  data: CodeType,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.post("/members/emails/verify", data).then(success).catch(fail);
};

const getUserDetail = async (
  memberId: string,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.get(`/members/${memberId}`).then(success).catch(fail);
};

const userInfoPatch = async (
  memberId: string,
  data: UserData,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.patch(`/members/${memberId}`, data).then(success).catch(fail);
};

const exitFindear = async (
  memberId: MemberId,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.patch(`/members/${memberId}`).then(success).catch(fail);
};

const searchMembers = async (
  keyword: string,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.get(`/members/?keyword=${keyword}`).then(success).catch(fail);
};

const findPassword = async (
  data: EmailProps,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.post(`/members/find-password`, data).then(success).catch(fail);
};

const resetPassword = async (
  memberId: string,
  data: resetPasswordType,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios
    .patch(`/members/${memberId}/password`, data)
    .then(success)
    .catch(fail);
};

const refreshToken = async (
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios.post("/members/token/refresh").then(success).catch(fail);
};

const nicknameCheck = async (
  nickname: string,
  success: (response: AxiosResponse) => void,
  fail: (error: any) => void
) => {
  await axios
    .get(`/members/nicknames/duplicate?nickname=${nickname}`)
    .then(success)
    .catch(fail);
};

export {
  signIn,
  signUp,
  signOut,
  checkEmail,
  sendCode,
  checkCode,
  getUserDetail,
  userInfoPatch,
  exitFindear,
  searchMembers,
  findPassword,
  resetPassword,
  refreshToken,
  nicknameCheck,
};
